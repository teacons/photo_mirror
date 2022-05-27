package settings

import Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import org.jetbrains.exposed.sql.transactions.transaction

@Composable
fun PhotoserverSettings(settings: Settings) {
    var photoserverEnabled by remember { mutableStateOf(settings.photoserverEnabled) }

    Column {
        Row {
            Checkbox(
                checked = photoserverEnabled == true,
                onCheckedChange = {
                    photoserverEnabled = it
                    transaction {
                        settings.photoserverEnabled = photoserverEnabled
                        commit()
                    }
                },
            )
            Text("Включен")
        }
    }
}