import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.io.File
import javax.imageio.ImageIO
import javax.print.PrintService
import javax.print.attribute.standard.Media
import javax.print.attribute.standard.MediaSize
import javax.print.attribute.standard.MediaSizeName
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.*

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

class ImagePrintable(private val image: BufferedImage) : Printable {
    override fun print(g: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
        g as Graphics2D
        g.translate(pageFormat.imageableX.toInt(), pageFormat.imageableY.toInt())
        if (pageIndex == 0) {
            val pageWidth = pageFormat.imageableWidth
            val pageHeight = pageFormat.imageableHeight
            val imageWidth = image.width
            val imageHeight = image.height



            val scaleX = pageWidth / imageWidth
            val scaleY = pageHeight / imageHeight
            val scaleFactor = min(scaleX, scaleY)
            g.scale(scaleFactor, scaleFactor)
            val dx = (pageWidth - imageWidth*scaleFactor) / 2
            val dy = (pageHeight - imageHeight*scaleFactor) / 2

            g.drawImage(image, (dx / scaleFactor).roundToInt(), (dy / scaleFactor).roundToInt(), null)
            return Printable.PAGE_EXISTS
        }
        return Printable.NO_SUCH_PAGE
    }
}

fun PrintService.getSupportedMediaSizeNames(): List<MediaSizeName> {
    return (getSupportedAttributeValues(Media::class.java, null, null) as Array<*>?)
        ?.filterIsInstance<MediaSizeName>()
        ?: emptyList()
}

fun PrintService.findMediaSizeName(mediaSizeName: String): MediaSizeName? {
    return getSupportedMediaSizeNames().find { it.toString() == mediaSizeName }
}

fun List<LayoutSettings>.findRelevant(mediaSize: MediaSize): List<LayoutSettings> {
    val mediaSizeWidth = mediaSize.getSize(MediaSize.MM)[0]
    val mediaSizeHeight = mediaSize.getSize(MediaSize.MM)[1]
    return filter {
        (max(mediaSizeWidth, it.sizeInPx.width.toFloat()) /
                min(mediaSizeWidth, it.sizeInPx.width.toFloat())).roundingTo(2) ==
                (max(mediaSizeHeight, it.sizeInPx.height.toFloat()) /
                        min(mediaSizeHeight, it.sizeInPx.height.toFloat())).roundingTo(2) ||
                (max(mediaSizeHeight, it.sizeInPx.width.toFloat()) /
                        min(mediaSizeHeight, it.sizeInPx.width.toFloat())).roundingTo(2) ==
                (max(mediaSizeWidth, it.sizeInPx.height.toFloat()) /
                        min(mediaSizeWidth, it.sizeInPx.height.toFloat())).roundingTo(2)
    }
}

fun Float.roundingTo(digit: Int): Float {
    return floor(this * 10.0f.pow(digit)) / 10.0f.pow(digit)
}