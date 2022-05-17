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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.consumeAllChanges
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
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.singleWindowApplication
import androidx.compose.ui.zIndex
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import org.burnoutcrew.reorderable.*
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

open class Layer(
    open val name: String,
    open var offset: MutableState<Offset>,
    open var scale: MutableState<Float>,
    open var rotation: MutableState<Float>,
)

data class PhotoLayer(
    override val name: String,
    override var offset: MutableState<Offset>,
    override var scale: MutableState<Float>,
    override var rotation: MutableState<Float>,
) : Layer(name, offset, scale, rotation)


data class TextLayer(
    override val name: String,
    override var offset: MutableState<Offset>,
    override var scale: MutableState<Float>,
    override var rotation: MutableState<Float>,
    val fontFamily: FontFamily,
    val fontSize: Int,
    val color: Color
) : Layer(name, offset, scale, rotation)

data class ImageLayer(
    override val name: String,
    override var offset: MutableState<Offset>,
    override var scale: MutableState<Float>,
    override var rotation: MutableState<Float>,
    val imageFile: File
) : Layer(name, offset, scale, rotation)


class EffectLayer(
    override val name: String,
    override var offset: MutableState<Offset>
) : Layer(name, offset, mutableStateOf(1f), mutableStateOf(0f))


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

@Composable
fun DraggableEditor(layers: List<Layer>, ratio: Float, selectedLayer: Layer?, onSelectedChange: (Layer) -> Unit) {
    Box(
        modifier = Modifier
            .background(Color.Gray)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .clipToBounds()
                .background(Color.White)
                .aspectRatio(ratio),
            contentAlignment = Alignment.Center
        ) {
            layers.forEachIndexed { index, layer ->
                val editingModifier =
                    Modifier
                        .offset { IntOffset(layer.offset.value.x.roundToInt(), layer.offset.value.y.roundToInt()) }
                        .pointerInput(layer) {
                            detectDragGestures(
                                onDragStart = { onSelectedChange(layer) },
                            ) { change, dragAmount ->
                                change.consumeAllChanges()
                                layer.offset.value += dragAmount
                            }
                        }
                        .graphicsLayer(
                            scaleX = layer.scale.value,
                            scaleY = layer.scale.value,
                            rotationZ = layer.rotation.value,
                        )
                        .then(if (selectedLayer == layer) Modifier.border(2.dp, Color.Black) else Modifier)
                        .clickable { onSelectedChange(layer) }
                        .zIndex(index.toFloat())

                when (layer) {
                    is PhotoLayer -> {}
                    is TextLayer -> {
                        Text(
                            text = layer.name,
                            fontFamily = layer.fontFamily,
                            fontSize = layer.fontSize.sp,
                            color = layer.color,
                            modifier = editingModifier
                        )
                    }
                    is ImageLayer -> {
                        Image(
                            bitmap = loadImageBitmap(layer.imageFile),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = editingModifier
                        )
                    }
                    is EffectLayer -> {}
                }
            }
        }
    }
}


@Composable
fun TextDialog(onCloseRequest: (TextLayer?) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var textError by remember { mutableStateOf(false) }
    var textFont by rememberSaveable { mutableStateOf("") }
    var fontError by remember { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableStateOf("") }
    var fontSizeError by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(Color.Black) }

    val dialogState = rememberDialogState(size = DpSize(500.dp, 700.dp))
    Dialog(
        onCloseRequest = { onCloseRequest(null) },
        title = "",
        state = dialogState
    ) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(8.dp)) {
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
            HarmonyColorPicker(
                harmonyMode = ColorHarmonyMode.SHADES,
                modifier = Modifier.size(400.dp),
                onColorChanged = { hsvColor ->
                    color = hsvColor.toColor()
                }
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
                                mutableStateOf(Offset.Zero),
                                mutableStateOf(1f),
                                mutableStateOf(0f),
                                FontFamily(Typeface(Typeface.makeFromName(textFont, FontStyle.NORMAL))),
                                fontSize.toInt(),
                                color
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

@Composable
fun ImageDialog(onCloseRequest: (ImageLayer?) -> Unit) {
    var imageFile by rememberSaveable { mutableStateOf<File?>(null) }
    var imageFileError by rememberSaveable { mutableStateOf(false) }
    val dialogState = rememberDialogState(size = DpSize(700.dp, 700.dp))
    Dialog(
        onCloseRequest = { onCloseRequest(null) },
        title = "",
        state = dialogState
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Файл изображения",
                style = MaterialTheme.typography.h6
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = if (imageFile == null) "" else imageFile!!.absolutePath,
                    onValueChange = {},
                    isError = imageFileError,
                    readOnly = true,
                    enabled = false,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.body1,
                )
                Button(
                    onClick = {
                        val chooser = JFileChooser()
                        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
                        chooser.fileFilter = FileNameExtensionFilter(
                            "Image files",
                            *ImageIO.getReaderFileSuffixes()
                        )
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            imageFile = File(chooser.selectedFile.path)
                        }
                    }
                ) {
                    Text(
                        text = "Выбрать",
                    )
                }
            }
            if (imageFile != null) {
                Image(
                    bitmap = loadImageBitmap(imageFile!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
            Button(
                onClick = {
                    if (imageFile != null)
                        onCloseRequest(
                            ImageLayer(
                                "Изображение ${imageFile!!.name}",
                                mutableStateOf(Offset.Zero),
                                mutableStateOf(1f),
                                mutableStateOf(0f),
                                imageFile!!
                            )
                        )
                    else {
                        imageFileError = true
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

    var textDialogIsVisible by remember { mutableStateOf(false) }
    var imageDialogIsVisible by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier.horizontalScroll(stateHorizontal).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolBarButton("Add Photo", Icons.Filled.Portrait) {
                onAddLayerListener(
                    PhotoLayer(
                        "Фото: ",
                        mutableStateOf(Offset.Zero),
                        mutableStateOf(1f),
                        mutableStateOf(0f)
                    )
                )
            }
            ToolBarButton("Add Text", Icons.Filled.Title) {
                textDialogIsVisible = true
            }
            ToolBarButton("Add Image", Icons.Filled.Image) {
                imageDialogIsVisible = true
            }
            ToolBarButton("Add Effect", Icons.Filled.PhotoFilter) {
                onAddLayerListener(EffectLayer("Эффект: ", mutableStateOf(Offset.Zero)))
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
    if (imageDialogIsVisible) ImageDialog { imageLayer ->
        imageDialogIsVisible = false
        if (imageLayer != null) {
            onAddLayerListener(imageLayer)
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

