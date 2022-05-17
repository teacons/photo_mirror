package layoutEditor

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.Title
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


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
fun ToolbarPreview() {
    MaterialTheme {
        ToolBar {}
    }
}