import org.jetbrains.exposed.sql.Table

object Settings : Table() {
    val cameraName = text("camera_name")

    //    ...
    val printerName = text("printer_name")

    //    ...
    val photoserverEnabled = bool("photoserver_enabled")

    //    ...
    val layoutCurrentId = integer("layout_current_id") //todo ref to layout table
    val guestHelloText = text("guest_hello_text")
    val guestShootText = text("guest_shoot_text")
    val guestWaitText = text("guest_wait_text")
    val guestShootTimer = integer("guest_shoot_timer")
    val guestBackgroundFilepath = text("guest_background_filepath")
    val guestTextFontFamily = text("guest_text_font_family")
    val guestTextFontSize = integer("guest_text_font_size")
    val guestTextFontColor = long("guest_text_font_color")
}
