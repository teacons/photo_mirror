package layoutEditor

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import org.burnoutcrew.reorderable.move
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import kotlin.math.roundToInt

fun main() = singleWindowApplication(WindowState(WindowPlacement.Maximized)) {
    MaterialTheme {
        LayoutEditor(LayoutSettings(emptyList(), IntSize(210, 297)), false) {}
    }
}

data class LayoutSettings(
    val layersList: List<Layer>,
    val size: IntSize,
)

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun LayoutEditor(layoutSettings: LayoutSettings, requestToClose: Boolean, onRequestToClose: (LayoutSettings) -> Unit) {
    val layersList = remember { mutableStateListOf(*layoutSettings.layersList.toTypedArray()) }
    val splitterState = rememberSplitPaneState(1f)
    val hSplitterState = rememberSplitPaneState(moveEnabled = false)
    val toolsSplitterState = rememberSplitPaneState(moveEnabled = false)
    var selectedLayer by remember { mutableStateOf<Layer?>(null) }
    var textDialogIsVisible by remember { mutableStateOf(false) }
    var imageDialogIsVisible by remember { mutableStateOf(false) }
    var photoDialogIsVisible by remember { mutableStateOf(false) }
    var sizeDraggableEditor by remember { mutableStateOf(IntSize.Zero) }

    if (requestToClose) onRequestToClose(LayoutSettings(layersList, sizeDraggableEditor))


    HorizontalSplitPane(splitPaneState = splitterState) {
        first(200.dp) {
            VerticalSplitPane(splitPaneState = hSplitterState) {
                first(100.dp) {
                    ToolBar(
                        onAddText = {
                            selectedLayer = null
                            textDialogIsVisible = true
                        },
                        onAddImage = {
                            selectedLayer = null
                            imageDialogIsVisible = true
                        },
                        onAddPhoto = {
                            selectedLayer = null
                            photoDialogIsVisible = true
                        }
                    )
                }
                second {
                    Box(
                        modifier = Modifier
                            .background(Color.Gray)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        DraggableEditor(
                            layers = layersList,
                            size = layoutSettings.size,
                            selectedLayer = selectedLayer,
                            onSelectedChange = { selectedLayer = it },
                            onSizeChange = { sizeDraggableEditor = it }
                        )
                    }
                }
            }
        }
        second(100.dp) {
            VerticalSplitPane(splitPaneState = toolsSplitterState) {
                first(if (selectedLayer == null) 0.dp else 150.dp) {
                    Box(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        if (selectedLayer != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Масштаб: ${(selectedLayer!!.scale.value * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.h6
                                )
                                Slider(
                                    value = selectedLayer!!.scale.value,
                                    onValueChange = { selectedLayer!!.scale.value = it },
                                    valueRange = 0f..3f
                                )
                                Text(
                                    text = "Поворот: ${selectedLayer!!.rotation.value}",
                                    style = MaterialTheme.typography.h6
                                )
                                Slider(
                                    value = selectedLayer!!.rotation.value,
                                    onValueChange = { selectedLayer!!.rotation.value = it },
                                    valueRange = 0f..360f
                                )
                            }
                        }
                    }
                }
                second {
                    LayersList(
                        layersList,
                        onLayersOrderChanged = { from, to ->
                            layersList.move(from.index, to.index)
                        },
                        onClick = { layer ->
                            selectedLayer = layer
                            when (layer) {
                                is ImageLayer -> {
                                    imageDialogIsVisible = true
                                }
                                is TextLayer -> {
                                    textDialogIsVisible = true
                                }
                                is PhotoLayer -> {
                                    photoDialogIsVisible = true
                                }
                            }
                        },
                        onDelete = {
                            layersList.remove(it)
                            if (layersList.isEmpty()) selectedLayer = null
                        }
                    )
                }
            }

        }
    }

    if (textDialogIsVisible) {
        TextDialog(selectedLayer as TextLayer?) { textLayer ->
            textDialogIsVisible = false
            if (textLayer != null) {
                if (selectedLayer != null) {
                    (selectedLayer as TextLayer).apply {
                        name = textLayer.name
                        fontFamily = textLayer.fontFamily
                        fontSize = textLayer.fontSize
                        color = textLayer.color
                    }
                } else
                    layersList.add(textLayer)
            }
        }
    }
    if (imageDialogIsVisible) {
        ImageDialog(selectedLayer as ImageLayer?) { imageLayer ->
            imageDialogIsVisible = false
            if (imageLayer != null) {
                if (selectedLayer != null) {
                    (selectedLayer as ImageLayer).apply {
                        name = imageLayer.name
                        imageFile = imageLayer.imageFile
                    }
                } else
                    layersList.add(imageLayer)
            }
        }
    }
    if (photoDialogIsVisible) {
        PhotoDialog(selectedLayer as PhotoLayer?) { photoLayer ->
            photoDialogIsVisible = false
            if (photoLayer != null) {
                if (selectedLayer != null) {
                    (selectedLayer as PhotoLayer).apply {
                        name = photoLayer.name
                        photoId = photoLayer.photoId
                        width = photoLayer.width
                        height = photoLayer.height
                    }
                } else
                    layersList.add(photoLayer)
            }
        }
    }
}


@Preview
@Composable
fun LayoutEditorPreview() {
    MaterialTheme {
        LayoutEditor(LayoutSettings(emptyList(), IntSize(210, 297)), false) {}
    }
}

