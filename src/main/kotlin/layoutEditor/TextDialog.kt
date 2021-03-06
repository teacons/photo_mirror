package layoutEditor

import Spinnable
import Spinner
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.awt.GraphicsEnvironment


@Composable
fun TextDialog(
    layer: TextLayer? = null,
    onCloseRequest: (TextLayer?) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(layer?.name ?: "") }
    var textError by rememberSaveable { mutableStateOf(false) }
    var textFont by rememberSaveable { mutableStateOf(layer?.fontFamily ?: "") }
    var fontError by rememberSaveable { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableStateOf(layer?.fontSize) }
    var fontSizeError by rememberSaveable { mutableStateOf(false) }
    var color by rememberSaveable { mutableStateOf(layer?.color ?: Color.Red) }

    val dialogState = rememberDialogState(size = DpSize(500.dp, 700.dp))
    Dialog(
        onCloseRequest = { onCloseRequest(null) },
        title = "",
        state = dialogState
    ) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(8.dp)) {
            val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
            Spinner(
                data = fonts.map { Font(it) },
                value = textFont,
                onSelectedChanges = { textFont = it.toString() },
                isError = fontError,
                label = { Text("??????????") }
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
                onValueChange = { fontSize = it.toIntOrNull() },
                isError = fontSizeError,
                label = { Text("????????????") }
            )
            HarmonyColorPicker(
                harmonyMode = ColorHarmonyMode.SHADES,
                modifier = Modifier.size(400.dp),
                onColorChanged = { hsvColor ->
                    color = hsvColor.toColor()
                },
                color = color
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                isError = textError,
                label = { Text("??????????") }
            )
            Button(
                onClick = {
                    if (text.isNotEmpty() && textFont.isNotEmpty() && fontSize != null
                    )
                        onCloseRequest(
                            TextLayer(
                                text,
                                mutableStateOf(Offset.Zero),
                                mutableStateOf(1f),
                                mutableStateOf(0f),
                                textFont,
                                fontSize!!,
                                color
                            )
                        )
                    else {
                        textError = text.isEmpty()
                        fontError = textFont.isEmpty()
                        fontSizeError = fontSize == null || fontSize == 0
                    }
                },
            ) {
                Text(text = "OK")
            }
        }
    }
}

@Preview
@Composable
fun TextDialogPreview() {
    MaterialTheme {
        TextDialog {}
    }
}

class Font(private val name: String) : Spinnable {
    override fun toString(): String = name
}