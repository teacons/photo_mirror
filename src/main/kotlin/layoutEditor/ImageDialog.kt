package layoutEditor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
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
import imageChooser
import loadImageBitmap


@Composable
fun ImageDialog(
    layer: ImageLayer? = null,
    onCloseRequest: (ImageLayer?) -> Unit
) {
    var imageFile by rememberSaveable { mutableStateOf(layer?.imageFile) }
    var imageFileError by rememberSaveable { mutableStateOf(false) }
    val dialogState = rememberDialogState(size = DpSize(700.dp, 700.dp))
    Dialog(
        onCloseRequest = { onCloseRequest(null) },
        title = "",
        state = dialogState
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Файл изображения",
                style = MaterialTheme.typography.h6
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = if (imageFile == null) "" else imageFile!!.absolutePath,
                    onValueChange = {},
                    isError = imageFileError,
                    readOnly = true,
                    enabled = false,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.body1,
                )
                Button(
                    onClick = { imageFile = imageChooser() }
                ) {
                    Text(
                        text = "Выбрать",
                    )
                }
            }
            if (imageFile != null) {
                Image(
                    bitmap = loadImageBitmap(imageFile!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
            Button(
                onClick = {
                    if (imageFile != null)
                        onCloseRequest(
                            ImageLayer(
                                "Изображение ${imageFile!!.name}",
                                mutableStateOf(Offset.Zero),
                                mutableStateOf(1f),
                                mutableStateOf(0f),
                                imageFile!!
                            )
                        )
                    else {
                        imageFileError = true
                    }
                },
            ) {
                Text(text = "OK")
            }
        }
    }
}