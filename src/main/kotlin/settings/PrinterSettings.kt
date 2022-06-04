package settings

import Layout
import Layouts
import Settings
import Spinnable
import Spinner
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import layoutEditor.DraggableEditor
import layoutEditor.PhotoLayer
import layoutEditor.PhotoLayerWithPhoto
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.attribute.standard.Media
import javax.print.attribute.standard.MediaSize
import javax.print.attribute.standard.MediaSizeName
import kotlin.math.floor

fun PrintService.getSupportedMediaSizeNames(): List<MediaSizeName> {
    return (getSupportedAttributeValues(Media::class.java, null, null) as Array<*>?)
        ?.filterIsInstance<MediaSizeName>()
        ?: emptyList()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PrinterSettings(settings: Settings) {
    val printServices = PrintServiceLookup.lookupPrintServices(null, null)

    var selectedPrintService by remember { mutableStateOf(printServices.find { it.name == settings.printerName }) }

    var selectedMediaSizeName by remember {
        mutableStateOf(
            selectedPrintService?.getSupportedMediaSizeNames()?.find { it.toString() == settings.printerMediaSizeName })
    }

    var selectedLayout by remember { mutableStateOf(transaction { settings.layout }) }


    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spinner(
            data = printServices.map {
                object : Spinnable {
                    override fun toString() = it.name
                }
            },
            value = selectedPrintService?.name ?: "",
            onSelectedChanges = {
                printServices.find { printService -> printService.name == it.toString() }?.let {
                    selectedPrintService = it
                    selectedMediaSizeName = null
                    transaction {
                        settings.printerName = selectedPrintService!!.name
                        settings.printerMediaSizeName = null
                        settings.layout = null
                        commit()
                    }
                }
            },
            label = { Text(text = "Принтер") }
        ) {
            Text(text = it.toString())
        }

        Spinner(
            data = selectedPrintService?.getSupportedMediaSizeNames()?.map {
                object : Spinnable {
                    override fun toString() = it.toString()
                }
            } ?: emptyList(),
            value = selectedMediaSizeName?.toString() ?: "",
            onSelectedChanges = {
                selectedPrintService?.getSupportedMediaSizeNames()
                    ?.find { mediaSizeName -> mediaSizeName.toString() == it.toString() }
                    ?.let {
                        selectedMediaSizeName = it
                        selectedLayout = null
                        transaction {
                            settings.printerMediaSizeName = selectedMediaSizeName!!.toString()
                            settings.layout = null
                            commit()
                        }
                    }
            },
            label = { Text(text = "Размер бумаги") }
        ) {
            Text(text = it.toString())
        }

        Spinner(
            data = selectedMediaSizeName?.let { selectedMediaSizeName ->
                transaction {
                    Layout.all().toList().filter {
                        MediaSize.getMediaSizeForName(selectedMediaSizeName)?.let {mediaSize ->
                            with(mediaSize.getSize(MediaSize.MM)) {
                                floor(get(0) / it.ratioWidth.toFloat()) == floor(get(1) / it.ratioHeight.toFloat()) ||
                                        floor(get(1) / it.ratioWidth.toFloat()) == floor(get(0) / it.ratioHeight.toFloat())
                            }
                        } ?: false
                    }
                }
            }?.map {
                object : Spinnable {
                    override fun toString() = it.name
                }
            } ?: emptyList(),
            value = selectedLayout?.name ?: "",
            onSelectedChanges = {
                transaction {
                    selectedLayout = Layout.find { Layouts.name eq it.toString() }.first()
                    settings.layout = selectedLayout!!
                    commit()
                }

            },
            label = { Text(text = "Совместимый макет") }
        ) {
            Text(text = it.toString())
        }

        val image by remember(selectedLayout) {
            selectedLayout?.width?.let { width ->
                selectedLayout?.height?.let { height ->
                    renderComposeScene(width, height) {
                        DraggableEditor(
                            selectedLayout!!.getLayers().map {
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
                            IntSize(width, height),
                            null,
                            {},
                            {}
                        )
                    }
                }
            }.let { mutableStateOf(it) }
        }

        if (image != null) {
            Box(
                modifier = Modifier
                    .aspectRatio(
                        MediaSize.getMediaSizeForName(selectedMediaSizeName)?.getSize(MediaSize.MM)?.let {
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