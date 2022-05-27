import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import guest.Guest
import settings.Settings


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
                    with(settings!!) {
                        Guest(
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