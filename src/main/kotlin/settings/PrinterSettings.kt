package settings

import LayoutSettings
import Spinnable
import Spinner
import ViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.unit.dp
import layoutEditor.DraggableEditor
import layoutEditor.PhotoLayer
import layoutEditor.PhotoLayerWithPhoto
import java.io.File
import javax.print.PrintService
import javax.print.attribute.standard.Media
import javax.print.attribute.standard.MediaSize
import javax.print.attribute.standard.MediaSizeName
import kotlin.math.floor

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
        floor(mediaSizeWidth / it.sizeInPx.width.toFloat()) == floor(mediaSizeHeight / it.sizeInPx.height.toFloat()) ||
                floor(mediaSizeHeight / it.sizeInPx.width.toFloat()) == floor(mediaSizeWidth / it.sizeInPx.height.toFloat())
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PrinterSettings() {
    val printServices = ViewModel.getPrintServices()

    val settings by ViewModel.settings.collectAsState()

    val printerSettings = settings.printerSettings


    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spinner(
            data = printServices.map {
                object : Spinnable {
                    override fun toString() = it.name
                }
            },
            value = printerSettings.printer?.name ?: "",
            onSelectedChanges = {
                ViewModel.updatePrinterSettings(
                    printerSettings.copy(
                        printer = ViewModel.findPrintService(it.toString()),
                        mediaSizeName = null,
                        layout = null
                    )
                )
            },
            label = { Text(text = "Принтер") }
        ) {
            Text(text = it.toString())
        }

        Spinner(
            data = printerSettings.printer?.getSupportedMediaSizeNames()?.map {
                object : Spinnable {
                    override fun toString() = it.toString()
                }
            } ?: emptyList(),
            value = printerSettings.mediaSizeName?.toString() ?: "",
            onSelectedChanges = {
                ViewModel.updatePrinterSettings(
                    printerSettings.copy(
                        mediaSizeName = printerSettings.printer!!.findMediaSizeName(it.toString()),
                        layout = null
                    )
                )
            },
            label = { Text(text = "Размер бумаги") }
        ) {
            Text(text = it.toString())
        }

        Spinner(
            data = printerSettings.mediaSizeName?.let { selectedMediaSizeName ->
                MediaSize.getMediaSizeForName(selectedMediaSizeName)?.let { mediaSize ->
                    ViewModel.getLayouts().findRelevant(mediaSize)
                }
            } ?: emptyList(),
            value = printerSettings.layout?.name ?: "",
            onSelectedChanges = {
                ViewModel.updatePrinterSettings(printerSettings.copy(layout = it as LayoutSettings))
            },
            label = { Text(text = "Совместимый макет") }
        ) {
            Text(text = it.toString())
        }

        val image by remember(printerSettings) {
            printerSettings.layout?.layoutSize?.let { size ->
                renderComposeScene(size.width, size.height) {
                    DraggableEditor(
                        printerSettings.layout.layers.map {
                            if (it is PhotoLayer) {
                                PhotoLayerWithPhoto(
                                    it.name,
                                    it.offset,
                                    it.scale,
                                    it.rotation,
                                    it.photoId,
                                    it.width,
                                    it.height,
                                    File(this.javaClass.classLoader.getResource("sample.png")!!.toURI())
                                )
                            } else it
                        },
                        size,
                        null,
                        {},
                        {}
                    )
                }
            }.let { mutableStateOf(it) }
        }

        if (image != null) {
            Box(
                modifier = Modifier
                    .aspectRatio(
                        MediaSize.getMediaSizeForName(printerSettings.mediaSizeName)?.getSize(MediaSize.MM)
                            ?.let {
                                if (image!!.width / image!!.height >= 1)
                                    it[1] / it[0]
                                else
                                    it[0] / it[1]
                            } ?: 1f)
                    .align(Alignment.CenterHorizontally)
                    .border(BorderStroke(2.dp, Color.Black))
            ) {
                Image(
                    bitmap = image!!.toComposeImageBitmap(),
                    contentDescription = null,
                )
            }
        }
    }
}