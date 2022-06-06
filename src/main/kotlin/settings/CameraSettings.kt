package settings

import Camera
import Spinnable
import Spinner
import ViewModel
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun CameraSettings() {

    val selectedCamera by ViewModel.camera.collectAsState()

    val cameraList by ViewModel.cameraList.collectAsState()

    val stateVertical = rememberScrollState(0)


    VerticalScrollbar(
        modifier = Modifier
            .fillMaxHeight(),
        adapter = rememberScrollbarAdapter(stateVertical)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.verticalScroll(stateVertical)
    ) {
        Spinner(
            data = cameraList,
            value = selectedCamera.cameraName,
            onSelectedChanges = {
                ViewModel.updateCurrentCamera(it as Camera)
            },
            label = { Text(text = "Камера") }
        ) {
            Text(text = it.toString())
        }

        selectedCamera.gP2Camera.config.filter {
            (it.path.startsWith("/main/imgsettings", ignoreCase = true) ||
                    it.path.startsWith("/main/capturesettings", ignoreCase = true))
                    && it.choices != null && it.choices.size > 1
        }.forEach { config ->
            Spinner(
                data = config.choices.map {
                    object : Spinnable {
                        override fun toString() = it
                    }
                },
                value = config.value.toString(),
                onSelectedChanges = {
                    selectedCamera.gP2Camera.setConfig(config.cloneWithNewValue(it.toString()))
                },
                label = { Text(text = config.label) }
            ) {
                Text(text = it.toString())
            }
        }
    }
}