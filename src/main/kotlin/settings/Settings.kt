package settings

import Settings
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.exposed.sql.transactions.transaction


@OptIn(ExperimentalSplitPaneApi::class)
@Composable
@Preview
fun Settings(onComplete: (Settings) -> Unit) {

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

    val menuItems = MenuItem.values()

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
                        transaction {
                            if (settings.isValid()) onComplete(settings)
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
                    MenuItem.Layout -> LayoutSettings()
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




