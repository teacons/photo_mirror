package settings

import Camera
import Spinnable
import Spinner
import ViewModel
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun CameraSettings() {

    val selectedCamera by ViewModel.camera.collectAsState()

    val cameraList by ViewModel.cameraList.collectAsState()

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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spinner(
                    data = cameraList,
                    value = selectedCamera?.cameraName ?: "",
                    onSelectedChanges = {
                        ViewModel.updateCurrentCamera(it as Camera)
                    },
                    label = { Text(text = "Камера") },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(text = it.toString())
                }
                IconButton(
                    onClick = { ViewModel.refreshCameraList() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                    )
                }
            }

            selectedCamera?.gP2Camera?.config?.filter {
                (it.path.startsWith("/main/imgsettings", ignoreCase = true) ||
                        it.path.startsWith("/main/capturesettings", ignoreCase = true))
                        && it.choices != null && it.choices.size > 1
            }?.forEach { config ->
                Spinner(
                    data = config.choices.map {
                        object : Spinnable {
                            override fun toString() = it
                        }
                    },
                    value = config.value.toString(),
                    onSelectedChanges = {
                        selectedCamera!!.gP2Camera.setConfig(config.cloneWithNewValue(it.toString()))
                    },
                    label = { Text(text = config.label) }
                ) {
                    Text(text = it.toString())
                }
            }
        }
    }
}