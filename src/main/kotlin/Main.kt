@file:OptIn(ExperimentalSplitPaneApi::class)

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import javax.print.PrintService
import javax.print.PrintServiceLookup

enum class MenuItem(val itemName: String) {
    PhotoCamera("Фотоаппарат"),
    Printer("Принтер"),
    PhotoServer("Фотосервер"),
    Layout("Макет")
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

@Composable
@Preview
fun Settings() {
    var workWithPhotoServer by remember { mutableStateOf(false) }
    var welcomeText by remember { mutableStateOf("") }

    var selectedCamera by rememberSaveable { mutableStateOf(CameraHelper("", null)) }
    var selectedPrinter by rememberSaveable { mutableStateOf(PrintServiceHelper(null)) }

    val splitterState = rememberSplitPaneState(moveEnabled = false)

    val menuItems = MenuItem.values()

    var selectedMenuItem by remember { mutableStateOf(MenuItem.PhotoCamera) }


    HorizontalSplitPane(
        splitPaneState = splitterState
    ) {
        first(200.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.surface)
            ) {
                menuItems.forEach {
                    MenuItem(it, selectedMenuItem) {
                        selectedMenuItem = it
                    }
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
                        LayoutSettings()
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
fun LayoutSettings() {
    Dialog(
        onCloseRequest = {}
    ) {
        LayoutEditor(210f / 297f)
    }
}

@Composable
fun PhotoCameraSettings(selectedCamera: CameraHelper, onSelectChanges: (CameraHelper) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Фотоаппарат",
                style = MaterialTheme.typography.h5
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
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


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Настройки"
    ) {
        MaterialTheme {
            Settings()
        }
    }
}




