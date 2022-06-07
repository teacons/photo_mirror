package settings

import Spinner
import ViewModel
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.awt.GraphicsEnvironment

@Composable
fun GuestSettings() {
    val settings by ViewModel.settings.collectAsState()
    val guestSettings = settings.guestSettings

    val stateVertical = rememberScrollState(0)

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(stateVertical)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.verticalScroll(stateVertical)
        ) {
            OutlinedTextField(
                value = guestSettings.guestHelloText ?: "",
                onValueChange = {
                    ViewModel.updateGuestSettings(guestSettings.copy(guestHelloText = it))
                },
                maxLines = 2,
                isError = guestSettings.guestHelloText.isNullOrEmpty(),
                label = { Text(text = "Текст приветствия") },
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )
            OutlinedTextField(
                value = guestSettings.guestShootText ?: "",
                onValueChange = {
                    ViewModel.updateGuestSettings(guestSettings.copy(guestShootText = it))
                },
                maxLines = 2,
                isError = guestSettings.guestShootText.isNullOrEmpty(),
                label = { Text(text = "Текст съёмки") },
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )
            OutlinedTextField(
                value = guestSettings.guestWaitText ?: "",
                onValueChange = {
                    ViewModel.updateGuestSettings(guestSettings.copy(guestWaitText = it))
                },
                maxLines = 2,
                isError = guestSettings.guestWaitText.isNullOrEmpty(),
                label = { Text(text = "Текст ожидания фотографии") },
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )
            OutlinedTextField(
                value = if (guestSettings.guestShootTimer == null) "" else guestSettings.guestShootTimer.toString(),
                onValueChange = {
                    ViewModel.updateGuestSettings(guestSettings.copy(guestShootTimer = it.toIntOrNull()))
                },
                maxLines = 1,
                isError = guestSettings.guestShootTimer == null,
                label = { Text(text = "Значение таймера") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = guestSettings.guestBackgroundFilepath ?: "",
                    onValueChange = {},
                    isError = guestSettings.guestBackgroundFilepath.isNullOrEmpty(),
                    readOnly = true,
                    enabled = false,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    label = { Text(text = "Файл изображения") }
                )
                Button(
                    onClick = {
                        ViewModel.updateGuestSettings(guestSettings.copy(guestBackgroundFilepath = imageChooser()?.absolutePath))
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
                value = guestSettings.guestTextFontFamily ?: "",
                onSelectedChanges = {
                    ViewModel.updateGuestSettings(guestSettings.copy(guestTextFontFamily = it.toString()))
                },
                isError = guestSettings.guestTextFontFamily.isNullOrEmpty(),
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
                value = if (guestSettings.guestTextFontSize == null) "" else guestSettings.guestTextFontSize.toString(),
                onValueChange = {
                    ViewModel.updateGuestSettings(guestSettings.copy(guestTextFontSize = it.toIntOrNull()))
                },
                maxLines = 1,
                isError = guestSettings.guestTextFontSize == null,
                label = { Text(text = "Размер шрифта") },
                modifier = Modifier.fillMaxWidth()
            )
            HarmonyColorPicker(
                harmonyMode = ColorHarmonyMode.SHADES,
                modifier = Modifier.size(350.dp),
                onColorChanged = { hsvColor ->
                    ViewModel.updateGuestSettings(guestSettings.copy(guestTextFontColor = hsvColor.toColor().value))
                },
                color = Color(guestSettings.guestTextFontColor ?: Color.Red.value)
            )
        }
    }
}