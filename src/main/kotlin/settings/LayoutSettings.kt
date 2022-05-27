package settings

import Layout
import LayoutImageLayer
import LayoutPhotoLayer
import LayoutTextLayer
import Settings
import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import layoutEditor.*
import org.jetbrains.exposed.sql.transactions.transaction


@Composable
fun LayoutSettings(settings: Settings, visible: Boolean, onCloseRequest: (LayoutSettings) -> Unit) {
    var requestToClose by remember { mutableStateOf(false) }
    if (visible) {
        Dialog(
            state = rememberDialogState(size = DpSize.Unspecified),
            onCloseRequest = { requestToClose = true }
        ) {
            LayoutEditor(
                transaction { settings.layout }?.let {
                    LayoutSettings(it.getLayers(), IntSize(it.width.toInt(), it.height.toInt()))
                },
                210f / 297f,
                requestToClose
            ) {
                transaction(db = settings.db) {
                    val layout = if (settings.layout == null) {
                        Layout.new {
                            name = "Temp"
                            width = it.size.width.toFloat()
                            height = it.size.height.toFloat()
                        }
                    } else {
                        settings.layout!!
                    }

                    layout.removeAllLayers()

                    it.layersList.forEachIndexed { index, layer ->
                        when (layer) {
                            is TextLayer -> LayoutTextLayer.new {
                                name = layer.name
                                offsetX = layer.offset.value.x
                                offsetY = layer.offset.value.y
                                scale = layer.scale.value
                                rotation = layer.rotation.value
                                fontFamily = layer.fontFamily
                                fontSize = layer.fontSize
                                fontColor = layer.color.value.toLong()
                                layoutId = layout
                                zIndex = index
                            }
                            is ImageLayer -> LayoutImageLayer.new {
                                name = layer.name
                                offsetX = layer.offset.value.x
                                offsetY = layer.offset.value.y
                                scale = layer.scale.value
                                rotation = layer.rotation.value
                                file = layer.imageFile.absolutePath
                                layoutId = layout
                                zIndex = index
                            }
                            is PhotoLayer -> LayoutPhotoLayer.new {
                                name = layer.name
                                offsetX = layer.offset.value.x
                                offsetY = layer.offset.value.y
                                scale = layer.scale.value
                                rotation = layer.rotation.value
                                photoId = layer.photoId
                                width = layer.width
                                height = layer.height
                                layoutId = layout
                                zIndex = index
                            }
                        }
                    }
                    settings.layout = layout
                    commit()
                }
                requestToClose = false
                onCloseRequest(it)
            }
        }
    }
}