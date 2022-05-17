package layoutEditor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import kotlin.math.roundToInt

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
                            fontFamily = FontFamily(
                                Typeface(
                                    Typeface.makeFromName(
                                        layer.fontFamily,
                                        FontStyle.NORMAL
                                    )
                                )
                            ),
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
                }
            }
        }
    }
}