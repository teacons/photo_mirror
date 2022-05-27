package settings

import Settings
import Spinnable
import Spinner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.jetbrains.exposed.sql.transactions.transaction


@Composable
fun CameraSettings(settings: Settings) {
    var cameraName by remember { mutableStateOf(settings.cameraName) }

    val cameraList = listOf(
        "Камера 1",
        "Камера 2",
        "Камера 3",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Фотоаппарат",
                style = MaterialTheme.typography.h5
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spinner(
                data = cameraList.map {
                    object : Spinnable {
                        override fun toString() = it
                    }
                },
                value = cameraName ?: "",
                onSelectedChanges = {
                    cameraName = it.toString()
                    if (!cameraName.isNullOrEmpty()) {
                        transaction {
                            settings.cameraName = cameraName
                            commit()
                        }
                    }
                },
            ) {
                Text(text = it.toString())
            }
        }
    }
}