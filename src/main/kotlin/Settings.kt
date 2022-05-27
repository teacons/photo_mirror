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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import layoutEditor.*
import main.Main
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.awt.GraphicsEnvironment
import javax.print.PrintServiceLookup

enum class MenuItem(val itemName: String) {
    PhotoCamera("Фотоаппарат"),
    Printer("Принтер"),
    PhotoServer("Фотосервер"),
    Layout("Макет"),
    GuestScreen("Экран работы")
}


@Composable
@Preview
fun Settings(onComplete: (Settings) -> Unit) {

    Database.connect("jdbc:sqlite:db.db", driver = "org.sqlite.JDBC")
    transaction {
        SchemaUtils.create(SettingsTable, Layouts, LayoutTextLayers, LayoutImageLayers, LayoutPhotoLayers)
        commit()
    }

    val settings by rememberSaveable {
        mutableStateOf(
            transaction() {
                try {
                    Settings.all().first()
                } catch (e: NoSuchElementException) {
                    Settings.new { }
                }
            }
        )
    }

    val splitterState = rememberSplitPaneState(moveEnabled = false)

    var selectedMenuItem by remember { mutableStateOf(MenuItem.PhotoCamera) }

    var layoutEditorIsVisible by remember { mutableStateOf(false) }

    val menuItems = MenuItem.values()

    LayoutSettings(settings, layoutEditorIsVisible) { layoutEditorIsVisible = false }

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
                        with(settings) {
                            if (cameraName != null && printerName != null && photoserverEnabled != null &&
                                layout != null && guestHelloText != null && guestShootText != null &&
                                guestWaitText != null && guestShootTimer != null && guestBackgroundFilepath != null &&
                                guestTextFontFamily != null && guestTextFontSize != null && guestTextFontColor != null
                            ) {
                                onComplete(settings)
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
                    MenuItem.PhotoCamera -> CameraSettings(settings)
                    MenuItem.Printer -> PrinterSettings(settings)
                    MenuItem.PhotoServer -> PhotoserverSettings(settings)
                    MenuItem.Layout -> {
                        layoutEditorIsVisible = true
                        selectedMenuItem = MenuItem.PhotoCamera
                    }
                    MenuItem.GuestScreen -> GuestScreenSettings(settings)

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
fun GuestScreenSettings(settings: Settings) {
    var guestHelloText by remember { mutableStateOf(settings.guestHelloText) }
    var guestHelloTextError by remember { mutableStateOf(settings.guestHelloText == null) }
    var guestShootTimer by remember { mutableStateOf(settings.guestShootTimer) }
    var guestShootTimerError by remember { mutableStateOf(settings.guestShootTimer == null) }
    var guestShootText by remember { mutableStateOf(settings.guestShootText) }
    var guestShootTextError by remember { mutableStateOf(settings.guestShootText == null) }
    var guestWaitText by remember { mutableStateOf(settings.guestWaitText) }
    var guestWaitTextError by remember { mutableStateOf(settings.guestWaitText == null) }
    var guestTextFontSize by remember { mutableStateOf(settings.guestTextFontSize) }
    var guestTextFontSizeError by remember { mutableStateOf(settings.guestTextFontSize == null) }
    var guestTextFontFamily by remember { mutableStateOf(settings.guestTextFontFamily) }
    var guestTextFontFamilyError by remember { mutableStateOf(settings.guestTextFontFamily == null) }
    var guestTextFontColor by remember { mutableStateOf(settings.guestTextFontColor) }
    var guestBackgroundFilepath by remember { mutableStateOf(settings.guestBackgroundFilepath) }
    var guestBackgroundFilepathError by remember { mutableStateOf(settings.guestBackgroundFilepath == null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = guestHelloText ?: "",
            onValueChange = {
                guestHelloText = it
                transaction {
                    settings.guestHelloText = guestHelloText
                    commit()
                }
                guestHelloTextError = guestHelloText.isNullOrEmpty()
            },
            maxLines = 2,
            isError = guestHelloTextError,
            label = { Text(text = "Текст приветствия") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        OutlinedTextField(
            value = guestShootText ?: "",
            onValueChange = {
                guestShootText = it
                transaction {
                    settings.guestShootText = guestShootText
                    commit()
                }
                guestShootTextError = guestShootText.isNullOrEmpty()
            },
            maxLines = 2,
            isError = guestShootTextError,
            label = { Text(text = "Текст съёмки") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        OutlinedTextField(
            value = guestWaitText ?: "",
            onValueChange = {
                guestWaitText = it
                transaction {
                    settings.guestWaitText = guestWaitText
                    commit()
                }
                guestWaitTextError = guestWaitText.isNullOrEmpty()
            },
            maxLines = 2,
            isError = guestWaitTextError,
            label = { Text(text = "Текст ожидания фотографии") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        OutlinedTextField(
            value = if (guestShootTimer == null) "" else guestShootTimer.toString(),
            onValueChange = {
                guestShootTimer = it.toIntOrNull()
                transaction {
                    settings.guestShootTimer = guestShootTimer
                    commit()
                }
                guestShootTimerError = guestShootTimer == null
            },
            maxLines = 1,
            isError = guestShootTimerError,
            label = { Text(text = "Значение таймера") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = guestBackgroundFilepath ?: "",
                onValueChange = {},
                isError = guestBackgroundFilepathError,
                readOnly = true,
                enabled = false,
                maxLines = 1,
                modifier = Modifier.weight(1f),
                label = { Text(text = "Файл изображения") }
            )
            Button(
                onClick = {
                    guestBackgroundFilepath = imageChooser()?.absolutePath
                    transaction {
                        settings.guestBackgroundFilepath = guestBackgroundFilepath
                        commit()
                    }
                    guestBackgroundFilepathError = guestBackgroundFilepath == null
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
            value = guestTextFontFamily ?: "",
            onSelectedChanges = {
                guestTextFontFamily = it.toString()
                transaction {
                    settings.guestTextFontFamily = guestTextFontFamily
                    commit()
                }
                guestTextFontFamilyError = guestTextFontFamily.isNullOrEmpty()
            },
            isError = guestTextFontFamilyError,
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
            value = if (guestTextFontSize == null) "" else guestTextFontSize.toString(),
            onValueChange = {
                guestTextFontSize = it.toIntOrNull()
                transaction {
                    settings.guestTextFontSize = guestTextFontSize
                    commit()
                }
                guestTextFontSizeError = guestTextFontSize == null
            },
            maxLines = 1,
            isError = guestTextFontSizeError,
            label = { Text(text = "Размер шрифта") },
            modifier = Modifier.fillMaxWidth()
        )
        HarmonyColorPicker(
            harmonyMode = ColorHarmonyMode.SHADES,
            modifier = Modifier.size(350.dp),
            onColorChanged = { hsvColor ->
                guestTextFontColor = hsvColor.toColor().value.toLong()
                transaction {
                    settings.guestTextFontColor = guestTextFontColor
                    commit()
                }
            },
            color = Color(guestTextFontColor?.toULong() ?: Color.Red.value)
        )
    }
}

@Composable
fun LayoutSettings(settings: Settings, visible: Boolean, onCloseRequest: (LayoutSettings) -> Unit) {
    var requestToClose by remember { mutableStateOf(false) }
    if (visible) {
        Dialog(
            state = rememberDialogState(size = DpSize.Unspecified),
            onCloseRequest = { requestToClose = true }
        ) {
            LayoutEditor(
                transaction { settings.layout }?.let {
                    LayoutSettings(it.getLayers(), IntSize(it.width.toInt(), it.height.toInt()))
                },
                210f / 297f,
                requestToClose
            ) {
                transaction(db = settings.db) {
                    val layout = if (settings.layout == null) {
                        Layout.new {
                            name = "Temp"
                            width = it.size.width.toFloat()
                            height = it.size.height.toFloat()
                        }
                    } else {
                        settings.layout!!
                    }

                    layout.removeAllLayers()

                    it.layersList.forEachIndexed { index, layer ->
                        when (layer) {
                            is TextLayer -> LayoutTextLayer.new {
                                name = layer.name
                                offsetX = layer.offset.value.x
                                offsetY = layer.offset.value.y
                                scale = layer.scale.value
                                rotation = layer.rotation.value
                                fontFamily = layer.fontFamily
                                fontSize = layer.fontSize
                                fontColor = layer.color.value.toLong()
                                layoutId = layout
                                zIndex = index
                            }
                            is ImageLayer -> LayoutImageLayer.new {
                                name = layer.name
                                offsetX = layer.offset.value.x
                                offsetY = layer.offset.value.y
                                scale = layer.scale.value
                                rotation = layer.rotation.value
                                file = layer.imageFile.absolutePath
                                layoutId = layout
                                zIndex = index
                            }
                            is PhotoLayer -> LayoutPhotoLayer.new {
                                name = layer.name
                                offsetX = layer.offset.value.x
                                offsetY = layer.offset.value.y
                                scale = layer.scale.value
                                rotation = layer.rotation.value
                                photoId = layer.photoId
                                width = layer.width
                                height = layer.height
                                layoutId = layout
                                zIndex = index
                            }
                        }
                    }
                    settings.layout = layout
                    commit()
                }
                requestToClose = false
                onCloseRequest(it)
            }
        }
    }
}

@Composable
fun PhotoserverSettings(settings: Settings) {
    var photoserverEnabled by remember { mutableStateOf(settings.photoserverEnabled) }

    Column {
        Row {
            Checkbox(
                checked = photoserverEnabled == true,
                onCheckedChange = {
                    photoserverEnabled = it
                    transaction {
                        settings.photoserverEnabled = photoserverEnabled
                        commit()
                    }
                },
            )
            Text("Включен")
        }
    }
}

@Composable
fun CameraSettings(settings: Settings) {
    var cameraName by remember { mutableStateOf(settings.cameraName) }

    val cameraList = listOf(
        "Камера 1",
        "Камера 2",
        "Камера 3",
    )

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
                data = cameraList.map {
                    object : Spinnable {
                        override fun toString() = it
                    }
                },
                value = cameraName ?: "",
                onSelectedChanges = {
                    cameraName = it.toString()
                    if (!cameraName.isNullOrEmpty()) {
                        transaction {
                            settings.cameraName = cameraName
                            commit()
                        }
                    }
                },
            ) {
                Text(text = it.toString())
            }
        }
    }
}

@Composable
fun PrinterSettings(settings: Settings) {
    var printerName by remember { mutableStateOf(settings.printerName) }

    val printServices = PrintServiceLookup.lookupPrintServices(null, null)

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
                data = printServices.map {
                    object : Spinnable {
                        override fun toString() = it.name
                    }
                },
                value = printerName ?: "",
                onSelectedChanges = {
                    printServices.find { printService -> printService.name == it.toString() }?.let {
                        printerName = it.name
                        transaction {
                            settings.printerName = printerName
                            commit()
                        }
                    }
                }
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

    var settings by remember { mutableStateOf<Settings?>(null) }

    when (state) {
        ApplicationState.Settings -> {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Настройки"
            ) {
                MaterialTheme {
                    Settings {
                        state = ApplicationState.Main
                        settings = it
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
                    with(settings!!) {
                        Main(
                            guestHelloText!!,
                            guestShootText!!,
                            guestWaitText!!,
                            guestBackgroundFilepath!!,
                            guestShootTimer!!,
                            guestTextFontFamily!!,
                            guestTextFontSize!!,
                            guestTextFontColor!!
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




