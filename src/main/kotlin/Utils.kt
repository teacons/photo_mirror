import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.io.File

fun loadImageBitmap(file: File): ImageBitmap = file.inputStream().buffered().use(::loadImageBitmap)