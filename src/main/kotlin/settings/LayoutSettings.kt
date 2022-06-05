package settings

import Layout
import LayoutImageLayer
import LayoutPhotoLayer
import LayoutTextLayer
import Layouts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import layoutEditor.*
import org.jetbrains.exposed.sql.transactions.transaction

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun LayoutSettings() {
    var layouts = remember {
        mutableStateListOf(*transaction {
            Layout.all().toList()
        }.toTypedArray())
    }
    var editorIsVisible by remember { mutableStateOf(false) }
    var selectedLayout by remember { mutableStateOf<Layout?>(null) }
    var layoutCreateIsVisible by remember { mutableStateOf(false) }
    if (editorIsVisible && selectedLayout != null) {
        DialogLayoutEditor(
            selectedLayout!!.toLayoutSettings()
        ) {
            transaction {
                selectedLayout!!.removeAllLayers()
                selectedLayout!!.layoutWidth = it.size.width
                selectedLayout!!.layoutHeight = it.size.height
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
                            layoutId = selectedLayout!!
                            zIndex = index
                        }
                        is ImageLayer -> LayoutImageLayer.new {
                            name = layer.name
                            offsetX = layer.offset.value.x
                            offsetY = layer.offset.value.y
                            scale = layer.scale.value
                            rotation = layer.rotation.value
                            file = layer.imageFile.absolutePath
                            layoutId = selectedLayout!!
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
                            layoutId = selectedLayout!!
                            zIndex = index
                        }
                    }
                }
                commit()
            }
            if (!layouts.contains(selectedLayout)) layouts.add(selectedLayout!!)
            else layouts[layouts.indexOf(selectedLayout)] =
                transaction { Layout.findById(selectedLayout!!.id)!! }
            editorIsVisible = false
        }
    }
    if (layoutCreateIsVisible) {
        DialogLayoutCreate(selectedLayout) { newLayout, updated ->
            when {
                selectedLayout == null && newLayout != null && updated -> { // новый
                    selectedLayout = newLayout
                    editorIsVisible = true
                }
                selectedLayout != null && newLayout == null && updated -> { // удаление
                    layouts.remove(selectedLayout)
                    selectedLayout = null
                }
                selectedLayout != null && newLayout != null && updated -> { // Обновление
                    println("kek")
                    layouts = mutableStateListOf(*transaction {
                        Layout.all().toList()
                    }.toTypedArray())

//                    layouts.remove(selectedLayout)
//                    selectedLayout = null
                }
            }
            layoutCreateIsVisible = false
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            cells = GridCells.Adaptive(minSize = 200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = layouts) { layout ->
                Box(
                    modifier = Modifier
                        .clickable {
                            selectedLayout = layout
                            editorIsVisible = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val lImage by remember(layout) {
                        layout.layoutWidth?.let { width ->
                            layout.layoutHeight?.let { height ->
                                renderComposeScene(width, height) {
                                    DraggableEditor(
                                        layout.getLayers(),
                                        IntSize(width, height),
                                        null,
                                        {},
                                        {}
                                    )
                                }
                            }
                        }.let { mutableStateOf(it) }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (lImage != null) {
                            Image(
                                bitmap = lImage!!.toComposeImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.border(BorderStroke(2.dp, Color.Black))
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = layout.name,
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    selectedLayout = layout
                                    layoutCreateIsVisible = true
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                layoutCreateIsVisible = true
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Text(text = "Новый макет")
        }
    }

}


@Composable
fun DialogLayoutEditor(layoutSettings: LayoutSettings, onCloseRequest: (LayoutSettings) -> Unit) {
    var requestToClose by remember { mutableStateOf(false) }
    Dialog(
        state = rememberDialogState(size = DpSize.Unspecified),
        onCloseRequest = { requestToClose = true }
    ) {
        LayoutEditor(
            layoutSettings,
            requestToClose
        ) {
            requestToClose = false
            onCloseRequest(it)
        }
    }
}

@Composable
fun DialogLayoutCreate(layout: Layout?, onFinish: (Layout?, Boolean) -> Unit) {
    var layoutName by remember { mutableStateOf(layout?.name ?: "") }
    var layoutNameError by remember { mutableStateOf(false) }
    var layoutWidth by remember { mutableStateOf(layout?.widthInPx?.toString() ?: "") }
    var layoutWidthError by remember { mutableStateOf(false) }
    var layoutHeight by remember { mutableStateOf(layout?.heightInPx?.toString() ?: "") }
    var layoutHeightError by remember { mutableStateOf(false) }
    Dialog(
        state = rememberDialogState(height = Dp.Unspecified),
        onCloseRequest = { onFinish(null, false) }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            OutlinedTextField(
                value = layoutName,
                onValueChange = {
                    layoutName = it
                    layoutNameError = layoutName.isEmpty()
                },
                maxLines = 1,
                isError = layoutNameError,
                label = { Text(text = "Название макета") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = layoutWidth,
                onValueChange = {
                    layoutWidth = it
                    layoutWidthError = layoutWidth.toIntOrNull() == null || layoutWidth.isEmpty()
                },
                maxLines = 1,
                isError = layoutWidthError,
                label = { Text(text = "Ширина макета") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = layoutHeight,
                onValueChange = {
                    layoutHeight = it
                    layoutHeightError = layoutHeight.toIntOrNull() == null || layoutHeight.isEmpty()
                },
                maxLines = 1,
                isError = layoutHeightError,
                label = { Text(text = "Высота макета") },
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Button(
                    onClick = {
                        if (layoutName.isNotEmpty() &&
                            transaction { Layout.find { Layouts.name eq layoutName }.empty() } &&
                            layoutWidth.toIntOrNull() != null && layoutHeight.toIntOrNull() != null
                        ) {
                            transaction {
                                onFinish(
                                    layout?.apply {
                                        name = layoutName
                                        widthInPx = layoutWidth.toInt()
                                        heightInPx = layoutHeight.toInt()
                                    } ?: Layout.new {
                                        name = layoutName
                                        widthInPx = layoutWidth.toInt()
                                        heightInPx = layoutHeight.toInt()
                                    },
                                    true
                                )
                                commit()
                            }
                        } else {
                            layoutNameError =
                                layoutName.isEmpty() || transaction {
                                    !Layout.find { Layouts.name eq layoutName }.empty()
                                }
                            layoutWidthError = layoutWidth.toFloatOrNull() == null
                            layoutHeightError = layoutHeight.toFloatOrNull() == null
                        }
                    }
                ) {
                    Text(text = "Ok")
                }
                if (layout != null) {
                    Button(
                        onClick = {
                            transaction {
                                layout.removeAllLayers()
                                layout.delete()
                                commit()
                            }
                            onFinish(null, true)
                        }
                    ) {
                        Text(text = "Удалить")
                    }
                }
            }
        }
    }
}