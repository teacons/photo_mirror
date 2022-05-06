import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.Title
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.burnoutcrew.reorderable.*
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.io.File
import kotlin.math.roundToInt

open class Layer(open val name: String)

class PhotoLayer(override val name: String) : Layer(name)

class TextLayer(override val name: String) : Layer(name)

class ImageLayer(
    override val name: String, val file: File?
) : Layer(name)

class EffectLayer(override val name: String) : Layer(name)

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

    HorizontalSplitPane(splitPaneState = splitterState) {
        first(200.dp) {
            VerticalSplitPane(splitPaneState = hSplitterState) {
                first(100.dp) {
                    ToolBar {
                        layersList.add(it)
                    }
                }
                second {
                    DraggableEditor(layersList, ratio)
                }
            }
        }
        second(100.dp) {
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

fun loadImageBitmap(file: File): ImageBitmap = file.inputStream().buffered().use(::loadImageBitmap)

@Composable
fun DraggableEditor(layers: List<Layer>, ratio: Float) {
    Box(
        modifier = Modifier.background(Color.Gray).fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.padding(4.dp)
                .clipToBounds()
                .background(Color.White).aspectRatio(ratio), contentAlignment = Alignment.Center
        ) {
            layers.forEach {
                when (it) {
                    is PhotoLayer -> {}
                    is TextLayer -> {
                        EditingBox {
                            Text(text = it.name)
                        }
                    }
                    is ImageLayer -> EditingBox {
                        if (it.file != null) Image(
                            bitmap = loadImageBitmap(it.file), contentDescription = null,
                            contentScale = ContentScale.Crop
                        ) else Text("File not found")
                    }
                    is EffectLayer -> {}
                }
            }
        }
    }
}

@Composable
fun EditingBox(Content: @Composable () -> Unit) {
    var offset by remember { mutableStateOf(Offset.Zero) }

    var scale by remember { mutableStateOf(Pair(1f, 1f)) }

    var oldSize by remember { mutableStateOf(Pair(100.dp, 100.dp)) }
    var newSize by remember { mutableStateOf(Pair(100.dp, 100.dp)) }

    var borderVisible by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier
        .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
        .pointerInput(Unit) {
            detectDragGestures(onDragStart = { borderVisible = true }) { change, dragAmount ->
                change.consumeAllChanges()
                offset += dragAmount
            }
        }
        .clickable { borderVisible = !borderVisible }
        .size(newSize.first, newSize.second)
        .then(if (borderVisible) Modifier.border(2.dp, Color.Black) else Modifier)

    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Content()
        }
        if (borderVisible) {
            Box(
                modifier = Modifier.background(Color.Black).align(Alignment.TopStart).size(10.dp).pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consumeAllChanges()
                            scale = Pair(
                                (-change.position.x.dp + oldSize.first) / oldSize.first,
                                (-change.position.y.dp + oldSize.second) / oldSize.second
                            )
                            newSize = Pair(oldSize.first * scale.first, oldSize.second * scale.second)
                        }, onDragEnd = {
                            oldSize = newSize
                        }, onDragCancel = {
                            oldSize = newSize
                        })
                }
            )
            Box(
                modifier = Modifier.background(Color.Black).align(Alignment.TopEnd).size(10.dp).pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consumeAllChanges()
                            scale = Pair(
                                (change.position.x.dp + oldSize.first) / oldSize.first,
                                (-change.position.y.dp + oldSize.second) / oldSize.second
                            )
                            newSize = Pair(oldSize.first * scale.first, oldSize.second * scale.second)
                        }, onDragEnd = {
                            oldSize = newSize
                        }, onDragCancel = {
                            oldSize = newSize
                        })
                }
            )
            Box(
                modifier = Modifier.background(Color.Black).align(Alignment.BottomStart).size(10.dp)
                    .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consumeAllChanges()
                            scale = Pair(
                                (-change.position.x.dp + oldSize.first) / oldSize.first,
                                (change.position.y.dp + oldSize.second) / oldSize.second
                            )
                            newSize = Pair(oldSize.first * scale.first, oldSize.second * scale.second)
                        }, onDragEnd = {
                            oldSize = newSize
                        }, onDragCancel = {
                            oldSize = newSize
                        })
                }
            )
            Box(
                modifier = Modifier.background(Color.Black).align(Alignment.BottomEnd).size(10.dp).pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consumeAllChanges()
                            scale = Pair(
                                (change.position.x.dp + oldSize.first) / oldSize.first,
                                (change.position.y.dp + oldSize.second) / oldSize.second
                            )
                            newSize = Pair(oldSize.first * scale.first, oldSize.second * scale.second)
                        }, onDragEnd = {
                            oldSize = newSize
                        }, onDragCancel = {
                            oldSize = newSize
                        })
                }
            )
        }
    }
}


@Composable
fun ToolBar(onAddLayerListener: (Layer) -> Unit) {
    val stateHorizontal = rememberScrollState(0)
    var imageCounter by remember { mutableStateOf(1) }
    Box {
        Row(
            modifier = Modifier.horizontalScroll(stateHorizontal).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolBarButton("Add Photo", Icons.Filled.Portrait) { onAddLayerListener(PhotoLayer("Фото: ")) }
            ToolBarButton("Add Text", Icons.Filled.Title) { onAddLayerListener(TextLayer("Текст: ")) }
            ToolBarButton("Add Image", Icons.Filled.Image) {
                onAddLayerListener(
                    ImageLayer(
                        "Изображение: $imageCounter", File("img.png")
                    )
                )
                imageCounter++
            }
            ToolBarButton("Add Effect", Icons.Filled.PhotoFilter) { onAddLayerListener(EffectLayer("Эффект: ")) }
        }
        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            adapter = rememberScrollbarAdapter(stateHorizontal)
        )
    }
}

@Composable
fun ToolBarButton(buttonName: String, buttonIcon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp).width(120.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = buttonIcon, contentDescription = null, modifier = Modifier.size(36.dp)
            )
            Text(text = buttonName, style = MaterialTheme.typography.subtitle1)
        }
    }
}

@Preview
@Composable
fun ToolbarEditorPreview() {
    MaterialTheme {
        ToolBar {}
    }
}

@Composable
fun LayersList(
    layersList: List<Layer>,
    onLayersOrderChanged: (ItemPosition, ItemPosition) -> Unit,
    onClick: (Layer) -> Unit
) {
    val state = rememberReorderState()

    LazyColumn(
        state = state.listState,
        modifier = Modifier.reorderable(
            state = state, onMove = onLayersOrderChanged
        )
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp),
    ) {
        itemsIndexed(layersList) { idx, item ->
            Card(
                modifier = Modifier
                    .clickable { onClick(item) }
                    .draggedItem(state.offsetByIndex(idx))
                    .detectReorder(state)
                    .fillParentMaxWidth()
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun LayoutEditorPreview() {
    MaterialTheme {
        LayoutEditor(210f / 297f)
    }
}

