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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState

enum class MenuItems(val itemName: String) {
    PhotoCamera("Фотоаппарат"),
    Printer("Принтер"),
    PhotoServer("Фотосервер")
}


@Composable
@Preview
fun Settings() {
    var workWithPhotoServer by remember { mutableStateOf(false) }
    var welcomeText by remember { mutableStateOf("") }

    var selectedCamera by rememberSaveable { mutableStateOf("") }
    var selectedPrinter by rememberSaveable { mutableStateOf("") }

    val splitterState = rememberSplitPaneState()

    val menuItems = MenuItems.values()

    var selectedMenuItem by remember { mutableStateOf(MenuItems.PhotoCamera) }


    HorizontalSplitPane(
        splitPaneState = splitterState
    ) {
        first(175.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.surface)
            ) {
                menuItems.forEach {
                    MenuItem(it) {
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
                    MenuItems.PhotoCamera -> PhotoCameraSettings(selectedCamera) { cameraName ->
                        selectedCamera = cameraName
                    }
                    MenuItems.Printer -> {
                        Box(Modifier)
                    }
                    MenuItems.PhotoServer -> {
                        Box(Modifier)
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
                        .background(MaterialTheme.colors.onBackground)
                )
            }

            handle {
                Box(Modifier)
            }

        }


    }

}

@Composable
fun PhotoCameraSettings(selectedCamera: String, onSelectChanges: (String) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Фотоаппарат",
                style = MaterialTheme.typography.h5
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val cameraList = listOf("Камера 1", "Камера 2", "Камера 3")
            Spinner(cameraList, selectedCamera, onSelectChanges)
        }
    }
}

@Composable
fun MenuItem(item: MenuItems, selected: Boolean = false, onMenuItemClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onMenuItemClick)
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = item.itemName,
            style = if (selected) MaterialTheme.typography.h6.copy(fontWeight = FontWeight.ExtraBold)
            else MaterialTheme.typography.h6
        )
    }
}

@Preview
@Composable
fun MenuItemPreview() {
    MaterialTheme {
        MenuItem(MenuItems.PhotoCamera) {}
    }
}


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
//        state = rememberWindowState(width = Dp.Unspecified, height = Dp.Unspecified),
//        resizable = false,
        title = "Настройки"
    ) {
        MaterialTheme {
            Settings()
        }
    }
}




