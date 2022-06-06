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
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import settings.Settings


@OptIn(ExperimentalComposeUiApi::class)
fun main() {


    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        module()
    }.start()

    application {

        var state by remember { mutableStateOf(ApplicationState.Settings) }

        when (state) {
            ApplicationState.Settings -> {
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "Настройки"
                ) {
                    MaterialTheme {
                        Settings {
                            state = ApplicationState.Main
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
                        Guest()
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