package settings

import Settings
import Spinnable
import Spinner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.exposed.sql.transactions.transaction
import javax.print.PrintServiceLookup

@Composable
fun PrinterSettings(settings: Settings) {
    var printerName by remember { mutableStateOf(settings.printerName) }

    val printServices = PrintServiceLookup.lookupPrintServices(null, null)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Принтер",
                style = MaterialTheme.typography.h5
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                }
            ) {
                Text(text = it.toString())
            }
        }
    }
}