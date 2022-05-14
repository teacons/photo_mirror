import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.singleWindowApplication
import org.burnoutcrew.reorderable.*
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.awt.GraphicsEnvironment
import java.io.File
import kotlin.math.roundToInt

open class Layer(
    open val name: String,
    open val offset: Offset
)

class PhotoLayer(
    override val name: String,
    override val offset: Offset
) : Layer(name, offset)


class TextLayer(
    override val name: String,
    override val offset: Offset,
    val fontFamily: FontFamily,
    val fontSize: Int,
    val color: Color
) : Layer(name, offset)

class ImageLayer(
    override val name: String,
    override val offset: Offset,
    val file: File?
) : Layer(name, offset)


class EffectLayer(
    override val name: String,
    override val offset: Offset
) : Layer(name, offset)


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
                .background(Color.White).aspectRatio(ratio),
            contentAlignment = Alignment.Center
        ) {
            layers.forEach {
                when (it) {
                    is PhotoLayer -> {}
                    is TextLayer -> {
                        EditingBox(onDimensionChange = { offset, scale ->

                        }) { _ ->
                            Text(
                                text = it.name,
                                fontFamily = it.fontFamily,
                                fontSize = it.fontSize.sp,
                                color = it.color
                            )
                        }
                    }
                    is ImageLayer -> EditingBox(onDimensionChange = { offset, scale ->

                    }) { scale ->
                        if (it.file != null) {
                            val image = loadImageBitmap(it.file)
                            Image(
                                bitmap = image, contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
//                                    .graphicsLayer(
//                                        scaleX = scale,
//                                        scaleY = scale
//                                    )
//                                    .size(image.width.dp*scale, image.height.dp*scale)
//                                    .scale(scale)
//                                    .background(Color.Red)
//                                    .aspectRatio((image.width/image.height).toFloat())
//                                    .fillMaxWidth()
//                                    .wrapContentSize()

                            )

                        } else Text("File not found")

                    }
                    is EffectLayer -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditingBox(
    modifier: Modifier = Modifier,
    initialOffset: Offset = Offset.Zero,
    initialScale: Float = 1f,
    onDimensionChange: (Offset, Float) -> Unit,
    Content: @Composable (Float) -> Unit
) {
    var offset by remember { mutableStateOf(initialOffset) }

    var scale by remember { mutableStateOf(1f) }

    val size by remember { mutableStateOf(DpSize.Unspecified) }

//    var oldSize by remember { mutableStateOf(Pair(100.dp, 100.dp)) }
//    var newSize by remember { mutableStateOf(Pair(100.dp, 100.dp)) }

    var borderVisible by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier
        .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
        .pointerInput(Unit) {
//            detectDragGestures(
//                onDragStart = { borderVisible = true },
////                onDragEnd = { onDimensionChange(offset, scale) },
////                onDragCancel = { onDimensionChange(offset, scale) }
//            ) { change, dragAmount ->
//                change.consumeAllChanges()
//                offset += dragAmount
//            }
        }
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale
        )
//        .size(size)
        .clickable { borderVisible = !borderVisible }

        .then(if (borderVisible) Modifier.border(2.dp, Color.Black) else Modifier)
        .wrapContentSize()
        .then(modifier)
//        .pointerInput(Unit) {
//            awaitPointerEventScope {
//                while (true) {
//                    val event = awaitPointerEvent()
//                    val position = event.changes.first().position
//                    // on every relayout Compose will send synthetic Move event,
//                    // so we skip it to avoid event spam
////                    if (event.type != PointerEventType.Move) {
//                        println("${event.type} $position")
////                    }
//                }
//            }}

    ) {
        val oldSize by remember { mutableStateOf(Pair(this.maxWidth, this.maxWidth)) }
        var newSize by remember { mutableStateOf(Pair(this.maxWidth, this.maxWidth)) }
//        var count by remember { mutableStateOf(0) }
//        var firstPos by remember { mutableStateOf(Offset(0f, 0f)) }

        Box(
            contentAlignment = Alignment.Center,
//            modifier = Modifier.scale(scale)
        ) {
            Content(scale)
        }
        if (borderVisible) {

            Box(
                modifier = Modifier.background(Color.Black).align(Alignment.TopStart).size(10.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            var pressed = false
                            var initOffset = Offset.Zero
                            var resultOffset = Offset.Zero
                            val width = maxWidth.toPx()
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes

                                val position = event.changes.first().position
                                // on every relayout Compose will send synthetic Move event,
                                // so we skip it to avoid event spam
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        pressed = true
                                        resultOffset = Offset.Zero
                                        initOffset = position
                                    }
                                    PointerEventType.Release -> {
                                        pressed = false
                                        resultOffset = Offset.Zero
                                        initOffset = Offset.Zero
                                    }
                                    PointerEventType.Move -> {
                                        if (pressed) {
//                                            scale = (position.x + initOffset.x) / initOffset.x
                                            println("initOffset: $initOffset | resultOffset: $position | scale: $scale | width: $width")
//                                            println(event.changes.map { it.position })
//                                            resultOffset += position
                                            scale = maxOf((-position.x + scale * width) / width, 0.3f)
                                        }
                                    }
                                }
                            }
                        }
                        /*detectDragGestures(
                            onDrag = { change, _ ->
                                if (count == 0) {
                                    count = 1
                                    firstPos = change.previousPosition
                                    println("firstPos = $firstPos")

    //                                return@detectDragGestures
                                }
                                change.consumeAllChanges()
    //                            change.position.getDistance()
    //                            scale = (sqrt(
    //                                (firstPos.x.toDouble() - change.position.x.toDouble()).pow(2.0) -
    //                                        (firstPos.x.toDouble() - change.position.x.toDouble()).pow(2.0)
    //                            ).dp + oldSize.first) / oldSize.first
                                val delta = change.previousPosition - change.position

                                scale = if (delta.x <= 0 || delta.y <= 0) {
    //                                 увеличение
                                    if (delta.x <= 0)
                                        (-abs(change.position.x).dp + oldSize.first) / oldSize.first
                                    else
                                        (abs(change.position.y).dp + oldSize.second) / oldSize.second
                                } else {
                                    // уменьшение
                                    (change.position.x.dp + oldSize.first) / oldSize.first
                                }
    //                            scale = (-change.position.x.dp + oldSize.first) / oldSize.first

    //                            scale =
    //                                (abs(change.position.getDistance()).dp +
    //                                        sqrt(oldSize.first.value + oldSize.second.value).dp) /
    //                                        sqrt(oldSize.first.value + oldSize.second.value).dp
                                println("change = $change")
                                println("x = ${change.position.x}")
                                println(scale)
    //                            count--
    //                            newSize = Pair(oldSize.first * scale, oldSize.second * scale)
                            }, onDragEnd = {
                                firstPos = Offset(0f, 0f)
                                count = 0
    //                            oldSize = newSize
    //                            onDimensionChange(offset, scale)
                            }, onDragCancel = {
    //                            oldSize = newSize
    //                            onDimensionChange(offset, scale)
                            })*/
                    }
            )
            Box(
                modifier = Modifier.background(Color.Black).align(Alignment.TopEnd).size(10.dp).pointerInput(Unit) {
                    /*         detectDragGestures(
                                 onDrag = { change, _ ->
                                     change.consumeAllChanges()
                                     scale =
                                         (change.position.x.dp + oldSize.first) / oldSize.first

         //                            newSize = Pair(oldSize.first * scale.first, oldSize.second * scale.second)
                                 }, onDragEnd = {
         //                            oldSize = newSize
                                 }, onDragCancel = {
         //                            oldSize = newSize
                                 })*/
                }
            )
            Box(
                modifier = Modifier.background(Color.Black).align(Alignment.BottomStart).size(10.dp)
                    .pointerInput(Unit) {
                        /*detectDragGestures(
                            onDrag = { change, _ ->
                                change.consumeAllChanges()
                                scale =
                                    (-change.position.x.dp + oldSize.first) / oldSize.first
//                                newSize = Pair(oldSize.first * scale.first, oldSize.second * scale.second)
                            }, onDragEnd = {
//                                oldSize = newSize
                            }, onDragCancel = {
//                                oldSize = newSize
                            })*/
                    }
            )
            Box(
                modifier = Modifier.background(Color.Black).align(Alignment.BottomEnd).size(10.dp).pointerInput(Unit) {
                    /*detectDragGestures(
                        onDrag = { change, _ ->
                            change.consumeAllChanges()
                            scale =
                                (change.position.x.dp + oldSize.first) / oldSize.first
//                            newSize = Pair(oldSize.first * scale.first, oldSize.second * scale.second)
                        }, onDragEnd = {
//                            oldSize = newSize
                        }, onDragCancel = {
//                            oldSize = newSize
                        })*/
                }
            )
        }
    }
}

var v = 0

@Composable
fun TextDialog(onCloseRequest: (TextLayer?) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var textError by remember { mutableStateOf(false) }
    var textFont by rememberSaveable { mutableStateOf("") }
    var fontError by remember { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableStateOf("") }
    var fontSizeError by remember { mutableStateOf(false) }

    Dialog(
        onCloseRequest = { onCloseRequest(null) },
        title = ""
    ) {
        Column {
            val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
            SearchableSpinner(
                data = fonts.map { Font(it) },
                onSelectedChanges = { textFont = it.toString() },
                isError = fontError,
                label = { Text("Шрифт") }
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
                value = fontSize,
                onValueChange = { fontSize = it },
                isError = fontSizeError,
                label = { Text("Размер") }
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                isError = textError,
                label = { Text("Текст") }
            )
            Button(
                onClick = {
                    if (text.isNotEmpty() && textFont.isNotEmpty() &&
                        (fontSize.isNotEmpty() && fontSize.toIntOrNull() != null)
                    )
                        onCloseRequest(
                            TextLayer(
                                text,
                                Offset.Zero,
                                FontFamily(Typeface(Typeface.makeFromName(textFont, FontStyle.NORMAL))),
                                fontSize.toInt(),
                                if (v == 0) {
                                    v++
                                    Color.Red
                                } else Color.Blue
                            )
                        )
                    else {
                        textError = text.isEmpty()
                        fontError = textFont.isEmpty()
                        fontSizeError = fontSize.isEmpty() || fontSize.toIntOrNull() == null
                    }
                },
            ) {
                Text(text = "OK")
            }
        }
    }
}

class Font(private val name: String) : Spinnable {
    override fun toString(): String = name
}

@Preview
@Composable
fun TextDialogPreview() {
    MaterialTheme {
        TextDialog {}
    }
}


@Composable
fun ToolBar(onAddLayerListener: (Layer) -> Unit) {
    val stateHorizontal = rememberScrollState(0)
    var imageCounter by remember { mutableStateOf(1) }

    var textDialogIsVisible by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier.horizontalScroll(stateHorizontal).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolBarButton("Add Photo", Icons.Filled.Portrait) {
                onAddLayerListener(PhotoLayer("Фото: ", Offset.Zero))
            }
            ToolBarButton("Add Text", Icons.Filled.Title) {
                textDialogIsVisible = true
//                onAddLayerListener(TextLayer("Текст: "))
            }
            ToolBarButton("Add Image", Icons.Filled.Image) {
                onAddLayerListener(
                    ImageLayer(
                        "Изображение: $imageCounter", Offset.Zero, File("img.png")
                    )
                )
                imageCounter++
            }
            ToolBarButton("Add Effect", Icons.Filled.PhotoFilter) {
                onAddLayerListener(EffectLayer("Эффект: ", Offset.Zero))
            }
        }
        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            adapter = rememberScrollbarAdapter(stateHorizontal)
        )
    }
    if (textDialogIsVisible) TextDialog { textLayer ->
        textDialogIsVisible = false
        if (textLayer != null) {
            onAddLayerListener(textLayer)
        }
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

