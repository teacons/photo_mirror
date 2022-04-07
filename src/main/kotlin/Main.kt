import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import javax.print.PrintServiceLookup


@Composable
@Preview
fun Settings() {
    var photoServer by remember { mutableStateOf(false) }
    var isPhotoLayoutSettingsOpen by remember { mutableStateOf(false) }
    var welcomeText by remember { mutableStateOf("") }

    var selectedCamera by rememberSaveable { mutableStateOf("") }
    var selectedPrinter by rememberSaveable { mutableStateOf("") }

    MaterialTheme {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Выбор фотоаппарата")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val cameraList = listOf("Камера 1", "Камера 2", "Камера 3")
                Spinner(cameraList, selectedCamera) { cameraName ->
                    selectedCamera = cameraName
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {

                Text("Выбор принтера")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val printServices = PrintServiceLookup.lookupPrintServices(null, null)
                val printerList = printServices.map { it.name }
                Spinner(printerList, selectedPrinter) { printerName ->
                    selectedPrinter = printerName
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Работа с фотосервером")
                Checkbox(
                    checked = photoServer,
                    onCheckedChange = { photoServer = it },
                    modifier = Modifier.padding(5.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { isPhotoLayoutSettingsOpen = true }
                ) {
                    Text(text = "Настройки макета фотографий")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Текст приветствия")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = welcomeText,
                    onValueChange = { welcomeText = it },
                    modifier = Modifier.defaultMinSize(minHeight = 100.dp)
                )
            }
        }

        if (isPhotoLayoutSettingsOpen) {
            Dialog(
                onCloseRequest = { isPhotoLayoutSettingsOpen = false },
                state = rememberDialogState(position = WindowPosition(Alignment.Center)),
                title = "Настройки макета фотографий"
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("TODO", textAlign = TextAlign.Center)
                }
            }
        }
    }
}





fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
//        state = rememberWindowState(width = Dp.Unspecified, height = Dp.Unspecified),
//        resizable = false,
        title = "Настройки"
    ) {
        Settings()
    }
}




