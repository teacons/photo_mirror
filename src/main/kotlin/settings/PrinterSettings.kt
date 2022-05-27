package settings

import Settings
import Spinnable
import Spinner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import org.jetbrains.exposed.sql.transactions.transaction
import javax.print.PrintServiceLookup

@Composable
fun PrinterSettings(settings: Settings) {
    var printerName by remember { mutableStateOf(settings.printerName) }

    val printServices = PrintServiceLookup.lookupPrintServices(null, null)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spinner(
            data = printServices.map {
                object : Spinnable {
                    override fun toString() = it.name
                }
            },
            value = printerName ?: "",
            onSelectedChanges = {
                printServices.find { printService -> printService.name == it.toString() }?.let {
                    printerName = it.name
                    transaction {
                        settings.printerName = printerName
                        commit()
                    }
                }
            },
            label = { Text(text = "Принтер") }
        ) {
            Text(text = it.toString())
        }
    }
}