import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object SettingsTable : IntIdTable() {
    val cameraName = text("camera_name").nullable()

    //    ...
    val printerName = text("printer_name").nullable()

    //    ...
    val photoserverEnabled = bool("photoserver_enabled").nullable()

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

//    var printerService by observable(
//        PrintServiceLookup.lookupPrintServices(null, null)
//            .find { printService -> printService.name == printerName }) { _, _, newValue ->
//        printerName = newValue?.name
//    }

    var photoserverEnabled by SettingsTable.photoserverEnabled

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
    val name = text("name")
    val width = float("width")
    val height = float("height")
}

class Layout(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Layout>(Layouts)

    val name by Layouts.name
    val width by Layouts.width
    val height by Layouts.height

    val textLayers by LayoutTextLayer referrersOn LayoutTextLayers.layoutId
    val imageLayers by LayoutImageLayer referrersOn LayoutImageLayers.layoutId
    val photoLayers by LayoutPhotoLayer referrersOn LayoutPhotoLayers.layoutId
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
    val zIndex = float("z_index")
}

class LayoutTextLayer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LayoutTextLayer>(LayoutTextLayers)

    val name by LayoutTextLayers.name
    val offsetX by LayoutTextLayers.offsetX
    val offsetY by LayoutTextLayers.offsetY
    val scale by LayoutTextLayers.scale
    val rotation by LayoutTextLayers.rotation
    val fontFamily by LayoutTextLayers.fontFamily
    val fontSize by LayoutTextLayers.fontSize
    val fontColor by LayoutTextLayers.fontColor
    val layoutId by Layout referencedOn LayoutTextLayers.layoutId
    val zIndex by LayoutTextLayers.zIndex
}

object LayoutImageLayers : IntIdTable() {
    val name = text("name")
    val offsetX = float("offset_x")
    val offsetY = float("offset_y")
    val scale = float("scale")
    val rotation = float("rotation")
    val file = text("file")
    val layoutId = reference("layout_id", Layouts)
    val zIndex = float("z_index")
}

class LayoutImageLayer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LayoutImageLayer>(LayoutImageLayers)

    val name by LayoutImageLayers.name
    val offsetX by LayoutImageLayers.offsetX
    val offsetY by LayoutImageLayers.offsetY
    val scale by LayoutImageLayers.scale
    val rotation by LayoutImageLayers.rotation
    val file by LayoutImageLayers.file
    var layoutId by Layout referencedOn LayoutImageLayers.layoutId
    val zIndex by LayoutImageLayers.zIndex
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
    val zIndex = float("z_index")
}

class LayoutPhotoLayer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LayoutPhotoLayer>(LayoutPhotoLayers)

    val name by LayoutPhotoLayers.name
    val offsetX by LayoutPhotoLayers.offsetX
    val offsetY by LayoutPhotoLayers.offsetY
    val scale by LayoutPhotoLayers.scale
    val rotation by LayoutPhotoLayers.rotation
    val photoId by LayoutPhotoLayers.photoId
    val width by LayoutPhotoLayers.width
    val height by LayoutPhotoLayers.height
    var layoutId by Layout referencedOn LayoutPhotoLayers.layoutId
    val zIndex by LayoutPhotoLayers.zIndex
}
