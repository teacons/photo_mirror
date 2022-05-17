package layoutEditor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState

@Composable
fun PhotoDialog(
    layer: PhotoLayer? = null,
    onCloseRequest: (PhotoLayer?) -> Unit
) {
    var photoId by rememberSaveable { mutableStateOf(layer?.photoId) }
    var photoIdError by rememberSaveable { mutableStateOf(false) }
    var photoWidth by rememberSaveable { mutableStateOf(layer?.width?.toInt()) }
    var photoWidthError by rememberSaveable { mutableStateOf(false) }
    var photoHeight by rememberSaveable { mutableStateOf(layer?.height?.toInt()) }
    var photoHeightError by rememberSaveable { mutableStateOf(false) }
    val dialogState = rememberDialogState(size = DpSize(500.dp, 400.dp))
    Dialog(
        onCloseRequest = { onCloseRequest(null) },
        title = "",
        state = dialogState
    ) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = if (photoId == null) "" else photoId.toString(),
                onValueChange = { photoId = it.toIntOrNull() },
                isError = photoIdError,
                label = { Text("Номер снимка") }
            )
            OutlinedTextField(
                value = if (photoWidth == null) "" else photoWidth.toString(),
                onValueChange = { photoWidth = it.toIntOrNull() },
                isError = photoWidthError,
                label = { Text("Ширина в пикселях") }
            )
            OutlinedTextField(
                value = if (photoHeight == null) "" else photoHeight.toString(),
                onValueChange = { photoHeight = it.toIntOrNull() },
                isError = photoHeightError,
                label = { Text("Высота в пикселях") }
            )
            Button(
                onClick = {
                    if (photoId != null && photoWidth != null && photoHeight != null)
                        onCloseRequest(
                            PhotoLayer(
                                "Снимок $photoId",
                                mutableStateOf(Offset.Zero),
                                mutableStateOf(1f),
                                mutableStateOf(0f),
                                photoId!!,
                                photoWidth!!.toFloat(),
                                photoHeight!!.toFloat()
                            )
                        )
                    else {
                        photoIdError = true
                        photoWidthError = true
                        photoHeightError = true
                    }
                },
            ) {
                Text(text = "OK")
            }
        }
    }
}