package layoutEditor

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.burnoutcrew.reorderable.move
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.io.File
import kotlin.math.roundToInt

fun main() = singleWindowApplication {
    MaterialTheme {
        LayoutEditor(210f / 297f)
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun LayoutEditor(ratio: Float) {
    val layersList = remember { mutableStateListOf<Layer>() }
    val splitterState = rememberSplitPaneState(1f)
    val hSplitterState = rememberSplitPaneState(moveEnabled = false)
    val toolsSplitterState = rememberSplitPaneState(moveEnabled = false)
    var selectedLayer by remember { mutableStateOf<Layer?>(null) }


    HorizontalSplitPane(splitPaneState = splitterState) {
        first(200.dp) {
            VerticalSplitPane(splitPaneState = hSplitterState) {
                first(100.dp) {
                    ToolBar {
                        layersList.add(it)
                    }
                }
                second {
                    DraggableEditor(
                        layersList,
                        ratio,
                        selectedLayer,
                        onSelectedChange = { selectedLayer = it })
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
                            layersList.remove(layer)
                        })
                }
            }

        }
    }
}

fun loadImageBitmap(file: File): ImageBitmap = file.inputStream().buffered().use(::loadImageBitmap)


@Preview
@Composable
fun LayoutEditorPreview() {
    MaterialTheme {
        LayoutEditor(210f / 297f)
    }
}

