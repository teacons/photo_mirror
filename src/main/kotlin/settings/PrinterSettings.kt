package settings

import LayoutSettings
import Spinnable
import Spinner
import ViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import findMediaSizeName
import findRelevant
import getSupportedMediaSizeNames
import java.io.File
import javax.print.attribute.standard.MediaSize


@Composable
fun PrinterSettings() {
    val printServices by ViewModel.printServices.collectAsState()

    val settings by ViewModel.settings.collectAsState()

    val printerSettings = settings.printerSettings



    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                label = { Text(text = "Принтер") },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(text = it.toString())
            }
            IconButton(
                onClick = { ViewModel.refreshPrintService() },
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                )
            }
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
            printerSettings.layout?.let {
                ViewModel.generateLayout(
                    it,
                    List(it.getCaptureCount()) {
                        File(this.javaClass.classLoader.getResource("sample.png")!!.toURI())
                    },
                    1f
                )
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
                    .fillMaxHeight()
            ) {
                Image(
                    bitmap = image!!.toComposeImageBitmap(),
                    contentDescription = null,
                )
            }
        }
    }
}