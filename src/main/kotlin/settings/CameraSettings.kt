package settings

import Settings
import Spinnable
import Spinner
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import x.mvmn.jlibgphoto2.api.CameraListItemBean
import x.mvmn.jlibgphoto2.api.GP2Camera
import x.mvmn.jlibgphoto2.impl.CameraDetectorImpl
import x.mvmn.jlibgphoto2.impl.GP2CameraImpl
import x.mvmn.jlibgphoto2.impl.GP2PortInfoList


data class Camera(
    val cameraListItem: CameraListItemBean,
    val cameraName: String,
) : Spinnable {
    var camera: GP2Camera? = null
    override fun toString(): String = cameraName
}


@Composable
fun CameraSettings(settings: Settings) {

    var selectedCamera by remember { mutableStateOf(settings.camera) }

    val isLinux by remember { mutableStateOf(System.getProperty("os.name").equals("Linux", true)) }


    val cameraList by remember {
        if (isLinux) {
            CameraDetectorImpl().detectCameras().map { Camera(it, it.cameraModel) }
        } else emptyList()
    }.let { mutableStateOf(it) }

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
            value = selectedCamera?.cameraName ?: "",
            onSelectedChanges = {
                selectedCamera?.camera?.close()
                selectedCamera = (it as Camera).apply {
                    camera = GP2CameraImpl(GP2PortInfoList().getByPath(cameraListItem.portName))
                }
                settings.camera = it
            },
            label = { Text(text = "Камера") }
        ) {
            Text(text = it.toString())
        }

        if (selectedCamera != null && selectedCamera!!.camera != null) {
            selectedCamera!!.camera!!.config.filter {
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
                        selectedCamera!!.camera!!.setConfig(config.cloneWithNewValue(it.toString()))
                    },
                    label = { Text(text = config.label) }
                ) {
                    Text(text = it.toString())
                }
            }
        }
    }
}