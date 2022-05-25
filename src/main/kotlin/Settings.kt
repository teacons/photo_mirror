@file:OptIn(ExperimentalSplitPaneApi::class)

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import layoutEditor.Font
import layoutEditor.LayoutEditor
import main.Main
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.awt.GraphicsEnvironment
import java.io.File
import javax.print.PrintService
import javax.print.PrintServiceLookup

enum class MenuItem(val itemName: String) {
    PhotoCamera("Фотоаппарат"),
    Printer("Принтер"),
    PhotoServer("Фотосервер"),
    Layout("Макет"),
    GuestScreen("Экран работы")
}

class PrintServiceHelper(
    val printService: PrintService?
) : Spinnable {

    override fun toString(): String = printService?.name ?: ""

}

class CameraHelper(
    private val cameraName: String,
//    val portInfo: String?
) : Spinnable {

    override fun toString(): String = cameraName

}

data class GuestSettings(
    val textWelcome: String?,
    val shootText: String?,
    val shootEndText: String?,
    val backgroundFile: File?,
    val initialTimer: Int?,
    val fontFamily: String?,
    val fontSize: Int?,
    val fontColor: Color?
)

data class CameraSettings(
    val cameraName: String?,
)

data class PrinterSettings(
    val printService: PrintService?,
)

data class PhotoserverSettings(
    val enabled: Boolean,

    )

@Composable
@Preview
fun Settings(onComplete: (GuestSettings) -> Unit) {

    Database.connect("jdbc:sqlite:db.db", driver = "org.sqlite.JDBC")
    transaction {
        SchemaUtils.create(Settings)
        commit()
    }

    val settingsFromDB = transaction {
        Settings.selectAll().singleOrNull()
    }

    var cameraSettings by rememberSaveable {
        mutableStateOf(
            settingsFromDB?.let { settingsFromDB ->
                CameraSettings(
                    settingsFromDB[Settings.cameraName]
                )
            }
        )
    }

    var printerSettings by rememberSaveable {
        mutableStateOf(
            settingsFromDB?.let { settingsFromDB ->
                PrintServiceLookup.lookupPrintServices(null, null)
                    .find { printService -> printService.name == settingsFromDB[Settings.printerName] }
                    ?.let {
                        PrinterSettings(it)
                    }
            }
        )
    }

    var photoserverSettings by rememberSaveable {
        mutableStateOf(
            settingsFromDB?.let { settingsFromDB ->
                PhotoserverSettings(settingsFromDB[Settings.photoserverEnabled])
            }
        )
    }

    val layoutCurrentId by rememberSaveable { mutableStateOf(settingsFromDB?.get(Settings.layoutCurrentId) ?: 0) }

    var guestSettings by rememberSaveable {
        mutableStateOf(settingsFromDB?.let {
            GuestSettings(
                it[Settings.guestHelloText],
                it[Settings.guestShootText],
                it[Settings.guestWaitText],
                File(it[Settings.guestBackgroundFilepath]),
                it[Settings.guestShootTimer],
                it[Settings.guestTextFontFamily],
                it[Settings.guestTextFontSize],
                Color(it[Settings.guestTextFontColor].toULong())
            )
        })
    }

    val splitterState = rememberSplitPaneState(moveEnabled = false)

    var selectedMenuItem by remember { mutableStateOf(MenuItem.PhotoCamera) }

    var layoutEditorIsVisible by remember { mutableStateOf(false) }

    val menuItems = MenuItem.values()

    LayoutSettings(layoutEditorIsVisible) { layoutEditorIsVisible = false }

    HorizontalSplitPane(
        splitPaneState = splitterState
    ) {
        first(200.dp) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.surface)
                        .align(Alignment.TopStart)
                ) {
                    menuItems.forEach {
                        MenuItem(it, selectedMenuItem) {
                            selectedMenuItem = it
                        }
                    }
                }
                Button(
                    onClick = {
                        if (guestSettings != null && cameraSettings != null && printerSettings != null &&
                            photoserverSettings != null
                        ) {
                            if (guestSettings!!.textWelcome != null && guestSettings!!.shootText != null &&
                                guestSettings!!.shootEndText != null && guestSettings!!.backgroundFile != null &&
                                guestSettings!!.initialTimer != null && guestSettings!!.fontFamily != null &&
                                guestSettings!!.fontSize != null && guestSettings!!.fontColor != null &&
                                cameraSettings!!.cameraName != null && printerSettings!!.printService != null
                            ) {
                                writeSettingsToDB(
                                    cameraSettings!!,
                                    printerSettings!!,
                                    photoserverSettings!!,
                                    layoutCurrentId,
                                    guestSettings!!
                                )
                                onComplete(guestSettings!!)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.9f)
                ) {
                    Text("Запуск")
                }
            }
        }
        second(50.dp) {
            Box(
                modifier = Modifier.padding(15.dp)
            ) {
                when (selectedMenuItem) {
                    MenuItem.PhotoCamera -> CameraSettings(cameraSettings) {
                        cameraSettings = it
                    }
                    MenuItem.Printer -> PrinterSettings(printerSettings) {
                        printerSettings = it
                    }
                    MenuItem.PhotoServer -> PhotoserverSettings(photoserverSettings) {
                        photoserverSettings = it
                    }
                    MenuItem.Layout -> {
                        layoutEditorIsVisible = true
                        selectedMenuItem = MenuItem.PhotoCamera
                    }
                    MenuItem.GuestScreen -> {
                        GuestScreenSettings(guestSettings) { guestSettings = it }
                    }
                }
            }
        }
        splitter {
            visiblePart {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colors.primary)
                )
            }

            handle {
                Box(Modifier)
            }
        }
    }
}

@Composable
fun GuestScreenSettings(guestSettings: GuestSettings?, onSettingsFinished: (GuestSettings) -> Unit) {
    var textWelcome by remember { mutableStateOf(guestSettings?.textWelcome) }
    var textWelcomeError by remember { mutableStateOf(if (guestSettings != null) guestSettings.textWelcome == null else false) }
    var initialTimer by remember { mutableStateOf(guestSettings?.initialTimer) }
    var initialTimerError by remember { mutableStateOf(if (guestSettings != null) guestSettings.initialTimer == null else false) }
    var shootText by remember { mutableStateOf(guestSettings?.shootText) }
    var shootTextError by remember { mutableStateOf(if (guestSettings != null) guestSettings.shootText == null else false) }
    var shootEndText by remember { mutableStateOf(guestSettings?.shootEndText) }
    var shootEndTextError by remember { mutableStateOf(if (guestSettings != null) guestSettings.shootEndText == null else false) }
    var fontSize by remember { mutableStateOf(guestSettings?.fontSize) }
    var fontSizeError by remember { mutableStateOf(if (guestSettings != null) guestSettings.fontSize == null else false) }
    var textFont by remember { mutableStateOf(guestSettings?.fontFamily) }
    var textFontError by remember { mutableStateOf(if (guestSettings != null) guestSettings.fontFamily == null else false) }
    var color by remember { mutableStateOf(guestSettings?.fontColor ?: Color.Red) }
    var imageFile by remember { mutableStateOf(guestSettings?.backgroundFile) }
    var imageFileError by remember { mutableStateOf(if (guestSettings != null) guestSettings.backgroundFile == null else false) }

    fun checkAndReturn() {
        onSettingsFinished(
            GuestSettings(
                textWelcome,
                shootText,
                shootEndText,
                imageFile,
                initialTimer,
                textFont,
                fontSize,
                color
            )
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = textWelcome ?: "",
            onValueChange = {
                textWelcome = it
                textWelcomeError = textWelcome.isNullOrEmpty()
                checkAndReturn()
            },
            maxLines = 2,
            isError = textWelcomeError,
            label = { Text(text = "Текст приветствия") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        OutlinedTextField(
            value = shootText ?: "",
            onValueChange = {
                shootText = it
                shootTextError = shootText.isNullOrEmpty()
                checkAndReturn()
            },
            maxLines = 2,
            isError = shootTextError,
            label = { Text(text = "Текст съёмки") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        OutlinedTextField(
            value = shootEndText ?: "",
            onValueChange = {
                shootEndText = it
                shootEndTextError = shootEndText.isNullOrEmpty()
                checkAndReturn()
            },
            maxLines = 2,
            isError = shootEndTextError,
            label = { Text(text = "Текст ожидания фотографии") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        OutlinedTextField(
            value = if (initialTimer == null) "" else initialTimer.toString(),
            onValueChange = {
                initialTimer = it.toIntOrNull()
                initialTimerError = initialTimer == null
                checkAndReturn()
            },
            maxLines = 1,
            isError = initialTimerError,
            label = { Text(text = "Значение таймера") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = if (imageFile == null) "" else imageFile!!.absolutePath,
                onValueChange = {},
                isError = imageFileError,
                readOnly = true,
                enabled = false,
                maxLines = 1,
                modifier = Modifier.weight(1f),
                label = { Text(text = "Файл изображения") }
            )
            Button(
                onClick = {
                    imageFile = imageChooser()
                    imageFileError = imageFile == null
                    checkAndReturn()
                }
            ) {
                Text(
                    text = "Выбрать",
                )
            }
        }
        val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
        Spinner(
            data = fonts.map { Font(it) },
            value = textFont ?: "",
            onSelectedChanges = {
                textFont = it.toString()
                textFontError = textFont.isNullOrEmpty()
                checkAndReturn()
            },
            isError = textFontError,
            label = { Text("Шрифт") },
        ) {
            Text(
                text = (it as Font).toString(),
                fontFamily = FontFamily(
                    Typeface(
                        Typeface.makeFromName(
                            it.toString(),
                            FontStyle.NORMAL
                        )
                    )
                )
            )
        }
        OutlinedTextField(
            value = if (fontSize == null) "" else fontSize.toString(),
            onValueChange = {
                fontSize = it.toIntOrNull()
                fontSizeError = fontSize == null
                checkAndReturn()
            },
            maxLines = 1,
            isError = fontSizeError,
            label = { Text(text = "Размер шрифта") },
            modifier = Modifier.fillMaxWidth()
        )
        HarmonyColorPicker(
            harmonyMode = ColorHarmonyMode.SHADES,
            modifier = Modifier.size(350.dp),
            onColorChanged = { hsvColor ->
                color = hsvColor.toColor()
                checkAndReturn()
            },
            color = color
        )
    }
}

@Composable
fun LayoutSettings(visible: Boolean, onCloseRequest: () -> Unit) {
    if (visible) {
        Dialog(
            state = rememberDialogState(size = DpSize.Unspecified),
            onCloseRequest = onCloseRequest
        ) {
            LayoutEditor(210f / 297f)
        }
    }
}

@Composable
fun PhotoserverSettings(photoserverSettings: PhotoserverSettings?, onSettingsFinished: (PhotoserverSettings) -> Unit) {
    var photoserverEnabled by remember { mutableStateOf(photoserverSettings?.enabled ?: false) }

    fun checkAndReturn() {
        onSettingsFinished(PhotoserverSettings(photoserverEnabled))
    }

    Column {
        Row {
            Checkbox(
                checked = photoserverEnabled,
                onCheckedChange = {
                    photoserverEnabled = !photoserverEnabled
                    checkAndReturn()
                },
            )
            Text("Включен")
        }
    }
}

@Composable
fun CameraSettings(cameraSettings: CameraSettings?, onSettingsFinished: (CameraSettings) -> Unit) {
    var selectedCamera by remember { mutableStateOf(cameraSettings?.cameraName ?: "") }

    val cameraList = listOf(
        CameraHelper("Камера 1"),
        CameraHelper("Камера 2"),
        CameraHelper("Камера 3"),
    )

    fun checkAndReturn() {
        onSettingsFinished(CameraSettings(selectedCamera))
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Фотоаппарат",
                style = MaterialTheme.typography.h5
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spinner(
                data = cameraList,
                value = selectedCamera,
                onSelectedChanges = {
                    selectedCamera = it.toString()
                    checkAndReturn()
                },
            ) {
                Text(text = it.toString())
            }
        }
    }
}

@Composable
fun PrinterSettings(printerSettings: PrinterSettings?, onSettingsFinished: (PrinterSettings) -> Unit) {
    var selectedPrinter by remember { mutableStateOf(printerSettings?.printService) }

    val printServices = PrintServiceLookup.lookupPrintServices(null, null)

    fun checkAndReturn() {
        if (selectedPrinter != null) onSettingsFinished(
            PrinterSettings(
                selectedPrinter!!
            )
        )
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Принтер",
                style = MaterialTheme.typography.h5
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spinner(
                data = printServices.map { PrintServiceHelper(it) },
                value = selectedPrinter?.name ?: "",
                onSelectedChanges = {
                    selectedPrinter = printServices.find { printService -> printService.name == it.toString() }
                    checkAndReturn()
                }
            ) {
                Text(text = it.toString())
            }
        }
    }
}

fun writeSettingsToDB(
    cameraSettings: CameraSettings,
    printerSettings: PrinterSettings,
    photoserverSettings: PhotoserverSettings,
    layoutId: Int,
    guestSettings: GuestSettings
) {
    transaction {
        Settings.deleteAll()
        Settings.insert {
            it[cameraName] = cameraSettings.cameraName!!
//                    ...
            it[printerName] = printerSettings.printService!!.name
//                    ...
            it[photoserverEnabled] = photoserverSettings.enabled
//                    ...
            it[layoutCurrentId] = layoutId
            it[guestHelloText] = guestSettings.textWelcome!!
            it[guestShootText] = guestSettings.shootText!!
            it[guestWaitText] = guestSettings.shootEndText!!
            it[guestShootTimer] = guestSettings.initialTimer!!
            it[guestBackgroundFilepath] = guestSettings.backgroundFile!!.absolutePath
            it[guestTextFontFamily] = guestSettings.fontFamily!!
            it[guestTextFontSize] = guestSettings.fontSize!!
            it[guestTextFontColor] = guestSettings.fontColor!!.value.toLong()
        }
        commit()
    }
}

@Composable
fun MenuItem(item: MenuItem, selectedItem: MenuItem, onMenuItemClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onMenuItemClick)
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = item.itemName,
            style = if (selectedItem == item) MaterialTheme.typography.h6.copy(fontWeight = FontWeight.ExtraBold)
            else MaterialTheme.typography.h6
        )
    }
}

@Preview
@Composable
fun MenuItemPreview() {
    MaterialTheme {
        MenuItem(MenuItem.PhotoCamera, MenuItem.PhotoCamera) {}
    }
}


@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {

    var state by remember { mutableStateOf(ApplicationState.Settings) }

    var guestSettings by remember { mutableStateOf<GuestSettings?>(null) }

    when (state) {
        ApplicationState.Settings -> {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Настройки"
            ) {
                MaterialTheme {
                    Settings {
                        state = ApplicationState.Main
                        guestSettings = it
                    }
                }
            }
        }
        ApplicationState.Main -> {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Настройки",
                state = rememberWindowState(placement = WindowPlacement.Fullscreen),
                onKeyEvent = {
                    if (it.key == Key.Escape) {
                        state = ApplicationState.Settings
                        true
                    } else false

                }
            ) {
                MaterialTheme {
//                    Main(
//                        textWelcome = "Бесплатное фото на память",
//                        initialTimer = 5,
//                        shootText = "Сыр",
//                        shootEndText = "Фото будет готово через 15 секунд",
//                        fontSize = 120,
//                        fontFamily = FontFamily(
//                            Typeface(
//                                Typeface.makeFromName(
//                                    "Comic Sans MS",
//                                    FontStyle.NORMAL
//                                )
//                            )
//                        ),
//                        fontColor = Color.White,
//                        File("background.png")
//                    )
                    with(guestSettings!!) {
                        Main(
                            textWelcome!!,
                            shootText!!,
                            shootEndText!!,
                            backgroundFile!!,
                            initialTimer!!,
                            fontFamily!!,
                            fontSize!!,
                            fontColor!!
                        )
                    }
                }
            }
        }
    }
}

enum class ApplicationState {
    Settings,
    Main
}




