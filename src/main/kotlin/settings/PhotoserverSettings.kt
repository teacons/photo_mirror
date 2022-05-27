package settings

import Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.validator.routines.InetAddressValidator
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.InetAddress

enum class AddressState {
    None, Good, Bad, Checking
}

@Composable
fun PhotoserverSettings(settings: Settings) {
    var photoserverEnabled by remember { mutableStateOf(settings.photoserverEnabled) }
    var photoserverAddress by remember { mutableStateOf(settings.photoserverAddress) }
    var photoserverAddressError by remember { mutableStateOf(settings.photoserverAddress == null) }
    var photoserverAddressState by remember { mutableStateOf(AddressState.None) }

    val coroutineScope = rememberCoroutineScope()

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
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
            if (photoserverEnabled == true) {
                Text("Включен")
            } else {
                Text("Выключен")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = photoserverAddress ?: "",
                onValueChange = {
                    photoserverAddress = it
                    photoserverAddressError = !InetAddressValidator.getInstance().isValid(it)
                    photoserverAddressState = AddressState.None
                },
                maxLines = 1,
                isError = photoserverAddressError,
                label = { Text(text = "Адрес фотосервера") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    photoserverAddressState = AddressState.Checking
                    coroutineScope.launch(context = Dispatchers.IO) {
                        if (!photoserverAddressError) {
                            if (InetAddress.getByAddress(photoserverAddress!!.split(".").map { it.toInt().toByte() }
                                    .toByteArray()).isReachable(5000)
                            ) {
                                photoserverAddressState = AddressState.Good
                                transaction {
                                    settings.photoserverAddress = photoserverAddress
                                    commit()
                                }
                            } else photoserverAddressState = AddressState.Bad
                        }
                    }
                }
            ) {
                Text(
                    text = "Проверить",
                )
            }
            Box(modifier = Modifier.size(40.dp)) {
                when (photoserverAddressState) {
                    AddressState.Good -> Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.matchParentSize()
                    )
                    AddressState.Bad -> Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.matchParentSize()
                    )
                    AddressState.Checking -> CircularProgressIndicator(
                        modifier = Modifier.matchParentSize()
                    )
                    AddressState.None -> {}
                }
            }
        }
    }
}