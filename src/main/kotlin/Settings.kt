@file:OptIn(ExperimentalSplitPaneApi::class)

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
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
    private val cameraModel: String,
    val portInfo: String?
) : Spinnable {

    override fun toString(): String = cameraModel

}

data class MainSettings(
    val textWelcome: String,
    val shootText: String,
    val shootEndText: String,
    val backgroundFile: File,
    val initialTimer: Int,
    val fontFamily: FontFamily,
    val fontSize: Int,
    val fontColor: Color
)

@Composable
@Preview
fun Settings(onComplete: (MainSettings) -> Unit) {
    var workWithPhotoServer by remember { mutableStateOf(false) }

    var selectedCamera by rememberSaveable { mutableStateOf(CameraHelper("", null)) }
    var selectedPrinter by rememberSaveable { mutableStateOf(PrintServiceHelper(null)) }

    val splitterState = rememberSplitPaneState(moveEnabled = false)

    val menuItems = MenuItem.values()

    var selectedMenuItem by remember { mutableStateOf(MenuItem.PhotoCamera) }

    var layoutEditorIsVisible by remember { mutableStateOf(false) }

    var mainSettings by remember { mutableStateOf<MainSettings?>(null) }


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
                    onClick = { mainSettings?.let { onComplete(it) } },
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
                    MenuItem.PhotoCamera -> PhotoCameraSettings(selectedCamera) { camera ->
                        selectedCamera = camera
//                        val camera = GPhoto2()
//                        camera.open()
//                        camera.capture() // image remains on camera
//
//                        val image: File = camera.captureAndDownload(false) // image saved to disk
//
//                        println(image.absolutePath)
//
//                        camera.close()
                    }
                    MenuItem.Printer -> PrinterSettings(selectedPrinter) { printerService ->
                        selectedPrinter = printerService
                    }
                    MenuItem.PhotoServer -> {
                        Box(Modifier)
                    }
                    MenuItem.Layout -> {
                        layoutEditorIsVisible = true
                        selectedMenuItem = MenuItem.PhotoCamera
                    }
                    MenuItem.GuestScreen -> {
                        GuestScreenSettings { mainSettings = it }
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
fun GuestScreenSettings(onSettingsFinished: (MainSettings) -> Unit) {
    var textWelcome by remember { mutableStateOf("") }
    var textWelcomeError by remember { mutableStateOf(false) }
    var initialTimer by remember { mutableStateOf<Int?>(null) }
    var initialTimerError by remember { mutableStateOf(false) }
    var shootText by remember { mutableStateOf("") }
    var shootTextError by remember { mutableStateOf(false) }
    var shootEndText by remember { mutableStateOf("") }
    var shootEndTextError by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf<Int?>(null) }
    var fontSizeError by remember { mutableStateOf(false) }
    var textFont by remember { mutableStateOf("") }
    var textFontError by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(Color.Red) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var imageFileError by remember { mutableStateOf(false) }

    fun checkAndReturn() {
        if (textWelcome.isNotEmpty() && initialTimer != null && shootText.isNotEmpty() && shootEndText.isNotEmpty()
            && fontSize != null && textFont.isNotEmpty() && imageFile != null
        ) onSettingsFinished(
            MainSettings(
                textWelcome,
                shootText,
                shootEndText,
                imageFile!!,
                initialTimer!!,
                FontFamily(
                    Typeface(
                        Typeface.makeFromName(
                            textFont,
                            FontStyle.NORMAL
                        )
                    )
                ),
                fontSize!!,
                color
            )
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = textWelcome,
            onValueChange = {
                textWelcome = it
                textWelcomeError = textWelcome.isEmpty()
                checkAndReturn()
            },
            maxLines = 2,
            isError = textWelcomeError,
            label = { Text(text = "Текст приветствия") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        OutlinedTextField(
            value = shootText,
            onValueChange = {
                shootText = it
                shootTextError = shootText.isEmpty()
                checkAndReturn()
            },
            maxLines = 2,
            isError = shootTextError,
            label = { Text(text = "Текст съёмки") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        OutlinedTextField(
            value = shootEndText,
            onValueChange = {
                shootEndText = it
                shootEndTextError = shootEndText.isEmpty()
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
        SearchableSpinner(
            data = fonts.map { Font(it) },
            value = textFont,
            onSelectedChanges = {
                textFont = it.toString()
                textFontError = textFont.isEmpty()
                checkAndReturn()
            },
            isError = textFontError,
            label = { Text("Шрифт") }
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
fun PhotoCameraSettings(selectedCamera: CameraHelper, onSelectChanges: (CameraHelper) -> Unit) {
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
            val cameraList = listOf(
                CameraHelper("Камера 1", null),
                CameraHelper("Камера 2", null),
                CameraHelper("Камера 3", null),
            )
            Spinner(
                cameraList, selectedCamera,
                onSelectedChanges = { onSelectChanges(it as CameraHelper) }
            ) {
                Text(text = it.toString())
            }
        }
    }
}

@Composable
fun PrinterSettings(selectedPrinter: PrintServiceHelper, onSelectChanges: (PrintServiceHelper) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Принтер",
                style = MaterialTheme.typography.h5
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val printServices = PrintServiceLookup.lookupPrintServices(null, null)
            val printerList = printServices.map { PrintServiceHelper(it) }
            Spinner(
                printerList, selectedPrinter,
                onSelectedChanges = { onSelectChanges(it as PrintServiceHelper) }
            ) {
                Text(text = it.toString())
            }
        }
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

    var mainSettings by remember { mutableStateOf<MainSettings?>(null) }

    when (state) {
        ApplicationState.Settings -> {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Настройки"
            ) {
                MaterialTheme {
                    Settings {
                        state = ApplicationState.Main
                        mainSettings = it
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
                    with(mainSettings!!) {
                        Main(
                            textWelcome,
                            shootText,
                            shootEndText,
                            backgroundFile,
                            initialTimer,
                            fontFamily,
                            fontSize,
                            fontColor
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




