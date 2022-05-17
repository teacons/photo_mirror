package layoutEditor

import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import java.io.File

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