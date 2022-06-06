import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import layoutEditor.DraggableEditor
import layoutEditor.PhotoLayer
import layoutEditor.PhotoLayerWithPhoto
import layoutEditor.TextLayer
import net.harawata.appdirs.AppDirsFactory
import org.apache.commons.validator.routines.InetAddressValidator
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import settings.getSupportedMediaSizeNames
import x.mvmn.jlibgphoto2.impl.CameraDetectorImpl
import x.mvmn.jlibgphoto2.impl.GP2CameraImpl
import x.mvmn.jlibgphoto2.impl.GP2PortInfoList
import java.awt.image.BufferedImage
import java.io.File
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.attribute.standard.MediaSizeName
import kotlin.math.roundToInt

object ViewModel {
    private val mutableStateFlowSettings: MutableStateFlow<ImmutableSettings>
    private val settingsFromDB: Settings
    val settings get() = mutableStateFlowSettings.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val mutableStateFlowCamera: MutableStateFlow<Camera>
    val camera get() = mutableStateFlowCamera.asStateFlow()

    private val mutableStateFlowCameraList: MutableStateFlow<List<Camera>>
    val cameraList get() = mutableStateFlowCameraList.asStateFlow()


    init {
        val appDirs = AppDirsFactory.getInstance()

        val userDataDir = appDirs.getUserDataDir("Photo Mirror", "1.0", "teacons")

        File(userDataDir).also { if (!it.exists()) it.mkdirs() }


        Database.connect("jdbc:sqlite:${userDataDir}${File.separator}db.db", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(SettingsTable, Layouts, LayoutTextLayers, LayoutImageLayers, LayoutPhotoLayers)
            commit()
        }

        settingsFromDB = transaction {
            try {
                Settings.all().first()
            } catch (e: NoSuchElementException) {
                Settings.new { }
            }
        }

        mutableStateFlowSettings = MutableStateFlow(settingsFromDB.toImmutableSettings())

        mutableStateFlowCameraList = MutableStateFlow(getCameraList())

        mutableStateFlowCamera = MutableStateFlow(mutableStateFlowCameraList.value.first())

        coroutineScope.launch {
            mutableStateFlowSettings.collect {
                settingsFromDB.updateWithImmutableSettings(it)
            }
        }
    }

    private fun getCameraList(): List<Camera> {
        return if (isLinux()) {
            CameraDetectorImpl().detectCameras()
                .map { Camera(GP2CameraImpl(GP2PortInfoList().getByPath(it.portName)), it.cameraModel) }
        } else emptyList()
    }

    fun getCameraByName(cameraName: String): Camera? {
        return cameraList.value.find { it.cameraName == cameraName }
    }

    private fun isLinux(): Boolean {
        return System.getProperty("os.name").equals("Linux", true)
    }

    fun updateSettings(settings: SettingsData) {
        mutableStateFlowSettings.value = ImmutableSettings(
            PrinterSettings(
                settings.printerName?.let { findPrintService(it) },
                settings.printerMediaSizeName?.let { mediaSizeName ->
                    settings.printerName?.let { printerName ->
                        findMediaSizeName(mediaSizeName, printerName)
                    }
                },
                settings.layout?.let { getLayoutByName(it) }),
            PhotoserverSettings(
                settings.photoserverEnabled == true,
                if (InetAddressValidator.getInstance().isValid(settings.photoserverAddress))
                    InetAddress.getByAddress(settings.photoserverAddress!!.split(".").map { it.toInt().toByte() }
                        .toByteArray())
                else null,
            ),
            GuestSettings(
                settings.guestHelloText,
                settings.guestShootText,
                settings.guestWaitText,
                settings.guestShootTimer,
                settings.guestBackgroundFilepath,
                settings.guestTextFontFamily,
                settings.guestTextFontSize,
                settings.guestTextFontColor
            )
        )
    }

    fun updateCurrentCamera(camera: Camera) {
        mutableStateFlowCamera.value = camera.copy()
    }

    fun updateGuestSettings(guestSettings: GuestSettings) {
        mutableStateFlowSettings.value = settings.value.copy(guestSettings = guestSettings)
    }

    fun updatePhotoServerSettings(photoserverSettings: PhotoserverSettings) {
        mutableStateFlowSettings.value = settings.value.copy(photoserverSettings = photoserverSettings)
    }

    fun updatePrinterSettings(printerSettings: PrinterSettings) {
        mutableStateFlowSettings.value = settings.value.copy(printerSettings = printerSettings)
    }

    fun getPrintServices(): List<PrintService> =
        PrintServiceLookup.lookupPrintServices(null, null)?.toList() ?: emptyList()

    fun getLayouts(): List<LayoutSettings> {
        return transaction { Layout.all().toList().map { it.toLayoutSettings() } }
    }

    fun getLayoutByName(layoutName: String): LayoutSettings {
        return transaction { Layout.find { Layouts.name eq layoutName }.first().toLayoutSettings() }
    }

    fun findPrintService(name: String): PrintService? = getPrintServices().find { it.name == name }

    fun findMediaSizeName(mediaSizeName: String, printServiceName: String): MediaSizeName? =
        findPrintService(printServiceName)?.getSupportedMediaSizeNames()?.find { it.toString() == mediaSizeName }

    fun captureImage(): File? {
        return try {
            val t = camera.value.gP2Camera.captureImage()
            val ba = camera.value.gP2Camera.getCameraFileContents(t.path, t.name)
            val sdf = SimpleDateFormat("dd-MM-yyyy")
            File("${sdf.format(Date())}${File.separator}${t.name}").apply {
                parentFile.mkdirs()
                createNewFile()
                outputStream().write(ba)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun cameraRelease() {
        camera.value.gP2Camera.release()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun generateLayout(layout: LayoutSettings, images: List<File>, requestedScale: Float? = null): BufferedImage {
        if (layout.getCaptureCount() != images.size) throw IllegalArgumentException("Capture count not equals image files size")
        val scale = requestedScale ?: (layout.sizeInPx.width.toFloat() / layout.layoutSize!!.width.toFloat())
        return renderComposeScene(
            (layout.layoutSize!!.width * scale).roundToInt(),
            (layout.layoutSize.height * scale).roundToInt()
        ) {
            DraggableEditor(
                layout.layers.map {
                    when (it) {
                        is PhotoLayer -> PhotoLayerWithPhoto(
                            it.name,
                            mutableStateOf(it.offset.value * scale),
                            it.scale,
                            it.rotation,
                            it.photoId,
                            it.width,
                            it.height,
                            images[it.photoId - 1]
                        )
                        is TextLayer -> it.apply {
                            it.offset = mutableStateOf(it.offset.value * scale)
                            it.fontSize = (it.fontSize * scale).roundToInt()
                        }

                        else -> it.apply { it.offset = mutableStateOf(it.offset.value * scale) }
                    }
                },
                IntSize(
                    (layout.layoutSize.width * scale).roundToInt(),
                    (layout.layoutSize.height * scale).roundToInt()
                ),
                null,
                {},
                {}
            )
        }.toComposeImageBitmap().toAwtImage()
    }
}