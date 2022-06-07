import ViewModel.findMediaSizeName
import ViewModel.findPrintService
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import layoutEditor.ImageLayer
import layoutEditor.Layer
import layoutEditor.PhotoLayer
import layoutEditor.TextLayer
import org.apache.commons.validator.routines.InetAddressValidator
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import x.mvmn.jlibgphoto2.api.GP2Camera
import java.io.File
import java.net.InetAddress
import javax.print.PrintService
import javax.print.attribute.standard.MediaSizeName

object SettingsTable : IntIdTable() {
    val printerName = text("printer_name").nullable()
    val printerMediaSizeName = text("printer_media_size_name").nullable()

    //    ...
    val photoserverEnabled = bool("photoserver_enabled").nullable()
    val photoserverAddress = text("photoserver_address").nullable()

    //    ...
    val layoutCurrentId = reference("layout_current_id", Layouts).nullable()
    val guestHelloText = text("guest_hello_text").nullable()
    val guestShootText = text("guest_shoot_text").nullable()
    val guestWaitText = text("guest_wait_text").nullable()
    val guestShootTimer = integer("guest_shoot_timer").nullable()
    val guestBackgroundFilepath = text("guest_background_filepath").nullable()
    val guestTextFontFamily = text("guest_text_font_family").nullable()
    val guestTextFontSize = integer("guest_text_font_size").nullable()
    val guestTextFontColor = long("guest_text_font_color").nullable()
}

object InetAddressSerializer : KSerializer<InetAddress?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InetAddress", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): InetAddress? {
        val inetAddressValidator = InetAddressValidator.getInstance()
        val string = decoder.decodeString()
        if (!inetAddressValidator.isValid(string)) return null
        val addressByteArray = string.split(".").map { it.toInt().toByte() }.toByteArray()
        return InetAddress.getByAddress(addressByteArray)
    }

    override fun serialize(encoder: Encoder, value: InetAddress?) {
        val string = value?.hostAddress ?: ""
        encoder.encodeString(string)
    }
}

object MediaSizeNameSerializer : KSerializer<MediaSizeName?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InetAddress", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): MediaSizeName? {
//        findMediaSizeName()
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, value: MediaSizeName?) {
        val string = value?.toString() ?: ""
        encoder.encodeString(string)
    }
}

data class Camera(
    val gP2Camera: GP2Camera,
    val cameraName: String,
) : Spinnable {
    override fun toString(): String = cameraName
}

data class GuestSettings(
    var guestHelloText: String?,
    var guestShootText: String?,
    var guestWaitText: String?,
    var guestShootTimer: Int?,
    var guestBackgroundFilepath: String?,
    var guestTextFontFamily: String?,
    var guestTextFontSize: Int?,
    var guestTextFontColor: ULong?
) {
    fun isValid(): Boolean {
        return guestHelloText != null && guestShootText != null && guestWaitText != null && guestShootTimer != null &&
                guestBackgroundFilepath != null && guestTextFontFamily != null && guestTextFontSize != null &&
                guestTextFontColor != null
    }
}

data class PhotoserverSettings(
    val photoserverEnabled: Boolean,
    val photoserverAddress: InetAddress?
)


data class PrinterSettings(
    val printer: PrintService?,
    val mediaSizeName: MediaSizeName?,
    val layout: LayoutSettings?,
) {
    fun isValid(): Boolean {
        return printer != null && mediaSizeName != null && layout != null
    }
}

data class ImmutableSettings(
    val printerSettings: PrinterSettings,

    val photoserverSettings: PhotoserverSettings,

    val guestSettings: GuestSettings
) {
    fun isValid(): Boolean {
        return printerSettings.isValid() && guestSettings.isValid()
    }

    fun toSettingsData(): SettingsData {
        return SettingsData(
            cameraName = ViewModel.camera.value?.cameraName,
            printerName = printerSettings.printer?.name,
            printerMediaSizeName = printerSettings.mediaSizeName?.toString(),
            photoserverEnabled = photoserverSettings.photoserverEnabled,
            photoserverAddress = photoserverSettings.photoserverAddress?.hostAddress,
            layout = printerSettings.layout?.name,
            guestHelloText = guestSettings.guestHelloText,
            guestShootText = guestSettings.guestShootText,
            guestWaitText = guestSettings.guestWaitText,
            guestShootTimer = guestSettings.guestShootTimer,
            guestBackgroundFilepath = guestSettings.guestBackgroundFilepath,
            guestTextFontFamily = guestSettings.guestTextFontFamily,
            guestTextFontSize = guestSettings.guestTextFontSize,
            guestTextFontColor = guestSettings.guestTextFontColor
        )
    }
}

class Settings(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Settings>(SettingsTable)

    var printerName by SettingsTable.printerName

    var printerMediaSizeName by SettingsTable.printerMediaSizeName

    var photoserverEnabled by SettingsTable.photoserverEnabled
    var photoserverAddress by SettingsTable.photoserverAddress

    var layout by Layout optionalReferencedOn SettingsTable.layoutCurrentId

    var guestHelloText by SettingsTable.guestHelloText
    var guestShootText by SettingsTable.guestShootText
    var guestWaitText by SettingsTable.guestWaitText
    var guestShootTimer by SettingsTable.guestShootTimer
    var guestBackgroundFilepath by SettingsTable.guestBackgroundFilepath
    var guestTextFontFamily by SettingsTable.guestTextFontFamily
    var guestTextFontSize by SettingsTable.guestTextFontSize
    var guestTextFontColor by SettingsTable.guestTextFontColor

    fun toImmutableSettings() =
        transaction {
            ImmutableSettings(
                PrinterSettings(
                    printer = printerName?.let { findPrintService(it) },
                    mediaSizeName = printerMediaSizeName?.let { mediaSizeName ->
                        printerName?.let { printerName ->
                            findMediaSizeName(mediaSizeName, printerName)
                        }
                    },
                    layout = layout?.toLayoutSettings(),
                ),
                PhotoserverSettings(
                    photoserverEnabled = photoserverEnabled ?: false,
                    photoserverAddress = InetAddress.getByAddress(
                        photoserverAddress?.split(".")?.map { it.toInt().toByte() }
                            ?.toByteArray()),
                ),
                GuestSettings(
                    guestHelloText = guestHelloText,
                    guestShootText = guestShootText,
                    guestWaitText = guestWaitText,
                    guestShootTimer = guestShootTimer,
                    guestBackgroundFilepath = guestBackgroundFilepath,
                    guestTextFontFamily = guestTextFontFamily,
                    guestTextFontSize = guestTextFontSize,
                    guestTextFontColor = guestTextFontColor?.toULong(),
                )
            )
        }

    fun updateWithImmutableSettings(immutableSettings: ImmutableSettings) {
        transaction {
            printerName = immutableSettings.printerSettings.printer?.name
            printerMediaSizeName = immutableSettings.printerSettings.mediaSizeName?.toString()
            layout = immutableSettings.printerSettings.layout?.let { Layout.find { Layouts.name eq it.name }.first() }
            photoserverEnabled = immutableSettings.photoserverSettings.photoserverEnabled
            photoserverAddress = immutableSettings.photoserverSettings.photoserverAddress?.hostAddress
            guestHelloText = immutableSettings.guestSettings.guestHelloText
            guestShootText = immutableSettings.guestSettings.guestShootText
            guestWaitText = immutableSettings.guestSettings.guestWaitText
            guestShootTimer = immutableSettings.guestSettings.guestShootTimer
            guestBackgroundFilepath = immutableSettings.guestSettings.guestBackgroundFilepath
            guestTextFontFamily = immutableSettings.guestSettings.guestTextFontFamily
            guestTextFontSize = immutableSettings.guestSettings.guestTextFontSize
            guestTextFontColor = immutableSettings.guestSettings.guestTextFontColor?.toLong()
        }
    }
}

@Serializable
data class CameraConfigEntry(
    val configName: String,
    val value: String,
    val choices: List<String>
)


@Serializable
data class SettingsData(
    var cameraName: String?,

    var printerName: String?,

    var printerMediaSizeName: String?,

    var photoserverEnabled: Boolean?,
    var photoserverAddress: String?,

    var layout: String?,

    var guestHelloText: String?,
    var guestShootText: String?,
    var guestWaitText: String?,
    var guestShootTimer: Int?,
    var guestBackgroundFilepath: String?,
    var guestTextFontFamily: String?,
    var guestTextFontSize: Int?,
    var guestTextFontColor: ULong?
)

object Layouts : IntIdTable() {
    val name = text("name").uniqueIndex()
    val width = integer("width").nullable()
    val height = integer("height").nullable()
    val ratioWidth = integer("ratio_width")
    val ratioHeight = integer("ratio_height")
}

data class LayoutSettings(
    val name: String,
    val layoutSize: IntSize?,
    val sizeInPx: IntSize,
    val layers: List<Layer>,
) : Spinnable {
    override fun toString(): String {
        return name
    }

    fun getCaptureCount(): Int {
        return layers.filterIsInstance<PhotoLayer>().maxOf { it.photoId }
    }

}

class Layout(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Layout>(Layouts)

    var name by Layouts.name
    var layoutWidth by Layouts.width
    var layoutHeight by Layouts.height
    var widthInPx by Layouts.ratioWidth
    var heightInPx by Layouts.ratioHeight

    private val textLayers by LayoutTextLayer referrersOn LayoutTextLayers.layoutId
    private val imageLayers by LayoutImageLayer referrersOn LayoutImageLayers.layoutId
    private val photoLayers by LayoutPhotoLayer referrersOn LayoutPhotoLayers.layoutId

    fun toLayoutSettings(): LayoutSettings {
        return LayoutSettings(
            name = name,
            layoutSize = layoutWidth?.let { layoutWidth ->
                layoutHeight?.let { layoutHeight ->
                    IntSize(layoutWidth, layoutHeight)
                }
            },
            sizeInPx = IntSize(widthInPx, heightInPx),
            layers = getLayers()
        )
    }

    fun removeAllLayers() {
        transaction {
            textLayers.forEach { it.delete() }
            imageLayers.forEach { it.delete() }
            photoLayers.forEach { it.delete() }
            commit()
        }
    }

    fun getLayers(): List<Layer> {
        return transaction {
            val layers =
                Array<Layer?>((textLayers.count() + imageLayers.count() + photoLayers.count()).toInt()) { null }

            textLayers.forEach {
                layers[it.zIndex] = it.toTextLayer()
            }
            imageLayers.forEach {
                layers[it.zIndex] = it.toImageLayer()
            }
            photoLayers.forEach {
                layers[it.zIndex] = it.toPhotoLayer()
            }

            layers
        }.filterNotNull()

    }
}

object LayoutTextLayers : IntIdTable() {
    val name = text("name")
    val offsetX = float("offset_x")
    val offsetY = float("offset_y")
    val scale = float("scale")
    val rotation = float("rotation")
    val fontFamily = text("font_family")
    val fontSize = integer("font_size")
    val fontColor = long("font_color")
    val layoutId = reference("layout_id", Layouts)
    val zIndex = integer("z_index")
}

class LayoutTextLayer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LayoutTextLayer>(LayoutTextLayers)

    var name by LayoutTextLayers.name
    var offsetX by LayoutTextLayers.offsetX
    var offsetY by LayoutTextLayers.offsetY
    var scale by LayoutTextLayers.scale
    var rotation by LayoutTextLayers.rotation
    var fontFamily by LayoutTextLayers.fontFamily
    var fontSize by LayoutTextLayers.fontSize
    var fontColor by LayoutTextLayers.fontColor
    var layoutId by Layout referencedOn LayoutTextLayers.layoutId
    var zIndex by LayoutTextLayers.zIndex

    fun toTextLayer(): TextLayer {
        return TextLayer(
            name,
            mutableStateOf(Offset(offsetX, offsetY)),
            mutableStateOf(scale),
            mutableStateOf(rotation),
            fontFamily,
            fontSize,
            Color(fontColor.toULong())
        )
    }
}

object LayoutImageLayers : IntIdTable() {
    val name = text("name")
    val offsetX = float("offset_x")
    val offsetY = float("offset_y")
    val scale = float("scale")
    val rotation = float("rotation")
    val file = text("file")
    val layoutId = reference("layout_id", Layouts)
    val zIndex = integer("z_index")
}

class LayoutImageLayer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LayoutImageLayer>(LayoutImageLayers)

    var name by LayoutImageLayers.name
    var offsetX by LayoutImageLayers.offsetX
    var offsetY by LayoutImageLayers.offsetY
    var scale by LayoutImageLayers.scale
    var rotation by LayoutImageLayers.rotation
    var file by LayoutImageLayers.file
    var layoutId by Layout referencedOn LayoutImageLayers.layoutId
    var zIndex by LayoutImageLayers.zIndex

    fun toImageLayer(): ImageLayer {
        return ImageLayer(
            name,
            mutableStateOf(Offset(offsetX, offsetY)),
            mutableStateOf(scale),
            mutableStateOf(rotation),
            File(file)
        )
    }
}

object LayoutPhotoLayers : IntIdTable() {
    val name = text("name")
    val offsetX = float("offset_x")
    val offsetY = float("offset_y")
    val scale = float("scale")
    val rotation = float("rotation")
    val photoId = integer("photo_id")
    val width = float("width")
    val height = float("height")
    val layoutId = reference("layout_id", Layouts)
    val zIndex = integer("z_index")
}

class LayoutPhotoLayer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LayoutPhotoLayer>(LayoutPhotoLayers)

    var name by LayoutPhotoLayers.name
    var offsetX by LayoutPhotoLayers.offsetX
    var offsetY by LayoutPhotoLayers.offsetY
    var scale by LayoutPhotoLayers.scale
    var rotation by LayoutPhotoLayers.rotation
    var photoId by LayoutPhotoLayers.photoId
    var width by LayoutPhotoLayers.width
    var height by LayoutPhotoLayers.height
    var layoutId by Layout referencedOn LayoutPhotoLayers.layoutId
    var zIndex by LayoutPhotoLayers.zIndex

    fun toPhotoLayer(): PhotoLayer {
        return PhotoLayer(
            name,
            mutableStateOf(Offset(offsetX, offsetY)),
            mutableStateOf(scale),
            mutableStateOf(rotation),
            photoId,
            width,
            height
        )
    }
}
