import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import layoutEditor.*
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object SettingsTable : IntIdTable() {
    val cameraName = text("camera_name").nullable()

    //    ...
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

class Settings(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Settings>(SettingsTable)

    var cameraName by SettingsTable.cameraName

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
}

object Layouts : IntIdTable() {
    val name = text("name").uniqueIndex()
    val width = integer("width").nullable()
    val height = integer("height").nullable()
    val ratioWidth = integer("ratio_width")
    val ratioHeight = integer("ratio_height")
}

class Layout(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Layout>(Layouts)

    var name by Layouts.name
    var width by Layouts.width
    var height by Layouts.height
    var ratioWidth by Layouts.ratioWidth
    var ratioHeight by Layouts.ratioHeight

    private val textLayers by LayoutTextLayer referrersOn LayoutTextLayers.layoutId
    private val imageLayers by LayoutImageLayer referrersOn LayoutImageLayers.layoutId
    private val photoLayers by LayoutPhotoLayer referrersOn LayoutPhotoLayers.layoutId

    fun toLayoutSettings(): LayoutSettings {
        return LayoutSettings(getLayers(), IntSize(ratioWidth, ratioHeight))
    }

    fun removeAllLayers() {
        transaction {
            textLayers.forEach { it.delete() }
            imageLayers.forEach { it.delete() }
            photoLayers.forEach { it.delete() }
            commit()
        }
    }

    fun getPhotoLayersMaxId(): Int {
        return transaction {
            photoLayers.maxOf { it.photoId }
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
