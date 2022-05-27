package settings

import Settings
import Spinner
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.unit.dp
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import imageChooser
import layoutEditor.Font
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.awt.GraphicsEnvironment

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