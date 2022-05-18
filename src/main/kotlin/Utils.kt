import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun loadImageBitmap(file: File): ImageBitmap = file.inputStream().buffered().use(::loadImageBitmap)

fun imageChooser(): File? {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.FILES_ONLY
    chooser.fileFilter = FileNameExtensionFilter(
        "Image files",
        *ImageIO.getReaderFileSuffixes()
    )
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) File(chooser.selectedFile.path)
    else null
}