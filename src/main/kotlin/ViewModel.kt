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
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.apache.commons.validator.routines.InetAddressValidator
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import x.mvmn.jlibgphoto2.impl.CameraDetectorImpl
import x.mvmn.jlibgphoto2.impl.GP2CameraImpl
import x.mvmn.jlibgphoto2.impl.GP2PortInfoList
import java.awt.image.BufferedImage
import java.io.File
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import javax.print.*
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.MediaPrintableArea
import javax.print.attribute.standard.MediaSize
import javax.print.attribute.standard.MediaSizeName
import javax.print.attribute.standard.OrientationRequested
import javax.print.event.PrintJobEvent
import javax.print.event.PrintJobListener
import kotlin.math.roundToInt
import kotlin.system.exitProcess

object ViewModel {
    private val mutableStateFlowSettings: MutableStateFlow<ImmutableSettings>
    private val settingsFromDB: Settings
    val settings get() = mutableStateFlowSettings.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val mutableStateFlowCamera: MutableStateFlow<Camera?>
    val camera get() = mutableStateFlowCamera.asStateFlow()

    private val mutableStateFlowCameraList: MutableStateFlow<List<Camera>>
    val cameraList get() = mutableStateFlowCameraList.asStateFlow()

    private val mutableStateFlowPrintServices = MutableStateFlow(getPrintServices())
    val printServices get() = mutableStateFlowPrintServices.asStateFlow()

    private val mutableStateFlowIsLocked = MutableStateFlow(false)
    val isLocked get() = mutableStateFlowIsLocked.asStateFlow()

    private var isCameraRefreshing = false
    private var isPrinterRefreshing = false

    val events = mutableListOf<Event>()

    private var prints = 1

    private var captures = 1

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

        mutableStateFlowCamera = MutableStateFlow(cameraList.value.firstOrNull())

        coroutineScope.launch {
            mutableStateFlowSettings.collect {
                settingsFromDB.updateWithImmutableSettings(it)
            }
        }
    }

    fun getEvents(lastId: Int): List<Event> {
        return events.subList(lastId, events.size)
    }

    fun addEvent(event: Event) {
        events.add(event)
    }

    fun getLastEventId(): Int {
        return events.size - 1
    }

//    fun eventIdIsValid(eventId: Int): Boolean {
//        return events.
//    }

    fun sendPhotoToPhotoserver(imageFile: File) {
        println("sending photo")
        val retrofit = Retrofit.Builder()
            .baseUrl("http://${settings.value.photoserverSettings.photoserverAddress!!.hostAddress}:8888")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(PhotoserverApi::class.java)
        println(settings.value.photoserverSettings.photoserverAddress!!.hostAddress)
        val t = MultipartBody.Part.createFormData(
            "img", imageFile.name, RequestBody.create(MediaType.parse("image/*"), imageFile.readBytes())
        )
        api.sendPhoto(t).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                println("finish ${response.code()}")
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                addEvent(Event("Отправка фото для фотосервера", "неудача"))
            }
        })
    }

    fun checkPhotoserverConnection(address: String): Boolean {
        return if (InetAddress.getByAddress(address.split(".").map { it.toInt().toByte() }.toByteArray())
                .isReachable(5000)
        ) {
            val url = URL("http://${address}:8888/mirror/check_connect")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
            }
            return try {
                conn.responseCode == HttpURLConnection.HTTP_OK
            } catch (e: ConnectException) {
                false
            }
        } else false
    }

    private fun getCameraList(): List<Camera> {
        return if (isLinux()) {
            try {
                CameraDetectorImpl().detectCameras()
                    .map { Camera(GP2CameraImpl(GP2PortInfoList().getByPath(it.portName)), it.cameraModel) }
            } catch (e: Exception) {
                addEvent(Event("Фотокамера", "getCameraList ${e.message}"))
                emptyList()
            }
        } else emptyList()
    }

    fun getCameraByName(cameraName: String): Camera? {
        return cameraList.value.find { it.cameraName == cameraName }
    }

    fun refreshCameraList() {
        if (!isCameraRefreshing) {
            isCameraRefreshing = true
            coroutineScope.launch {
                try {
                    mutableStateFlowCameraList.value.forEach {
                        try {
                            it.gP2Camera.close()
                        } catch (_: Exception) {
                        }
                    }
                    mutableStateFlowCameraList.value = getCameraList()
                    mutableStateFlowCamera.value = cameraList.value.firstOrNull()
                } catch (e: Exception) {
                    addEvent(Event("Фотокамера", "refreshCameraList ${e.message}"))
                }
                isCameraRefreshing = false
            }
        }
    }

    fun refreshPrintService() {
        if (!isPrinterRefreshing) {
            isPrinterRefreshing = true
            coroutineScope.launch {
                mutableStateFlowPrintServices.value = getPrintServices()
                isPrinterRefreshing = false
            }
        }

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

    fun updateCameraConfiguration(cameraConfiguration: List<CameraConfigEntry>) {
        if (camera.value != null) {
            try {
                val k = camera.value!!.gP2Camera.config
                val t = cameraConfiguration.map { cameraConfigEntry ->
                    k.find { it.label == cameraConfigEntry.configName }!!.cloneWithNewValue(cameraConfigEntry.value)
                }
                camera.value!!.gP2Camera.setConfig(*t.toTypedArray())
            } catch (e: Exception) {
                addEvent(Event("Фотокамера", "updateCameraConfiguration ${e.message}"))
            }
        }
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
        return if (camera.value != null) try {
            val t = camera.value!!.gP2Camera.captureImage()
            val ba = camera.value!!.gP2Camera.getCameraFileContents(t.path, t.name)
            val sdf = SimpleDateFormat("dd-MM-yyyy")
            val file = File(
                "${sdf.format(Date())}${File.separator}original${File.separator}$prints-$captures.${
                    t.name.split(".").last()
                }"
            ).apply {
                parentFile.mkdirs()
                createNewFile()
                outputStream().write(ba)
            }
            captures++
            file
        } catch (e: Exception) {
            addEvent(Event("Фотокамера", "captureImage ${e.message}"))
            null
        } else null
    }

    fun capturesToZero() {
        captures = 0
    }

    fun cameraRelease() {
        if (camera.value != null) {
            try {
                camera.value!!.gP2Camera.release()
            } catch (e: Exception) {
                addEvent(Event("Фотокамера", " ${e.message}"))
            }
        }
    }

    fun lockMirror(): Boolean {
        mutableStateFlowIsLocked.value = true
        return isLocked.value
    }

    fun unlockMirror(): Boolean {
        mutableStateFlowIsLocked.value = false
        return isLocked.value
    }

    fun shutdownMirror() {
        exitProcess(0)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun generateLayout(layout: LayoutSettings, images: List<File>, requestedScale: Float? = null): BufferedImage {
        if (layout.getCaptureCount() != images.size) throw IllegalArgumentException("Capture count not equals image files size")
        val scale = requestedScale ?: (layout.sizeInPx.width.toFloat() / layout.layoutSize!!.width.toFloat())
        val image = renderComposeScene(
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

        val sdf = SimpleDateFormat("dd-MM-yyyy")
        val file = File("${sdf.format(Date())}${File.separator}prints${File.separator}$prints.png").apply {
            parentFile.mkdirs()
            createNewFile()
        }

        ImageIO.write(image, "PNG", file)

        prints++

        return image
    }

    fun print(print: BufferedImage) {
        val printer = settings.value.printerSettings.printer!!
        val mediaSizeName = settings.value.printerSettings.mediaSizeName!!
        val job = printer.createPrintJob()
        job.addPrintJobListener(object : PrintJobListener {
            override fun printDataTransferCompleted(pje: PrintJobEvent?) {
            }

            override fun printJobCompleted(pje: PrintJobEvent?) {
            }

            override fun printJobFailed(pje: PrintJobEvent?) {
                addEvent(
                    Event(
                        subject = "Принтер",
                        reason = "Ошибка печати, повторите"
                    )
                )
            }

            override fun printJobCanceled(pje: PrintJobEvent?) {
                addEvent(
                    Event(
                        subject = "Принтер",
                        reason = "Задание на печать было отменено пользователем или другой программой"
                    )
                )
            }

            override fun printJobNoMoreEvents(pje: PrintJobEvent?) {
            }

            override fun printJobRequiresAttention(pje: PrintJobEvent?) {
                addEvent(
                    Event(
                        subject = "Принтер",
                        reason = "Требуется внимание (возможно закончилась бумага или краска)"
                    )
                )
            }
        })
        val printAttributes = HashPrintRequestAttributeSet().apply {
            if (print.width >= print.height) add(OrientationRequested.LANDSCAPE)
            else add(OrientationRequested.PORTRAIT)
            add(mediaSizeName)

            MediaSize.getMediaSizeForName(mediaSizeName).getSize(MediaSize.INCH).let {
                add(MediaPrintableArea(0f, 0f, it[0], it[1], MediaPrintableArea.INCH))
            }
        }
        val doc = SimpleDoc(
            ImagePrintable(print),
            DocFlavor.SERVICE_FORMATTED.PRINTABLE,
            null
        )
        try {
            job.print(doc, printAttributes)
        } catch (e: PrintException) {
            e.printStackTrace()
        }
    }
}