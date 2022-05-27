package layoutEditor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import loadImageBitmap
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}

@Composable
fun DraggableEditor(layers: List<Layer>, ratio: Float, selectedLayer: Layer?, onSelectedChange: (Layer) -> Unit, onSizeChange: (IntSize) -> Unit) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .clipToBounds()
                .background(Color.White)
                .aspectRatio(ratio)
                .onSizeChanged(onSizeChange)
            ,
            contentAlignment = Alignment.Center
        ) {
            layers.forEachIndexed { index, layer ->
                val editingModifier =
                    Modifier
                        .offset { IntOffset(layer.offset.value.x.roundToInt(), layer.offset.value.y.roundToInt()) }
                        .scale(layer.scale.value)
                        .rotate(layer.rotation.value)
                        .pointerInput(layer) {
                            detectDragGestures(
                                onDragStart = { onSelectedChange(layer) },
                            ) { change, dragAmount ->
                                change.consumeAllChanges()
                                layer.offset.value =
                                    ((layer.offset.value / layer.scale.value) + dragAmount.rotateBy(layer.rotation.value)) * layer.scale.value
                            }
                        }
                        .then(if (selectedLayer == layer) Modifier.border(2.dp, Color.Black) else Modifier)
                        .clickable { onSelectedChange(layer) }
                        .zIndex(index.toFloat())

                when (layer) {
                    is PhotoLayer -> {
                        Box(
                            modifier = editingModifier
                                .size(with(LocalDensity.current) { DpSize(layer.width.toDp(), layer.height.toDp()) })
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = layer.photoId.toString(),
                                style = MaterialTheme.typography.h3,
                                color = Color.White
                            )
                        }
                    }
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