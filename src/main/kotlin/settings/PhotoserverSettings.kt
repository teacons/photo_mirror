package settings

import IpConnectionTextField
import ViewModel
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress


@Composable
fun PhotoserverSettings() {
    val settings by ViewModel.settings.collectAsState()

    val photoserverSettings = settings.photoserverSettings

    var photoserverAddress by rememberSaveable(photoserverSettings) { mutableStateOf(photoserverSettings.photoserverAddress?.hostAddress) }

    var photoserverInetAddress by rememberSaveable { mutableStateOf<InetAddress?>(null) }

    val stateVertical = rememberScrollState(0)

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(stateVertical)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.verticalScroll(stateVertical)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = photoserverSettings.photoserverEnabled,
                    onCheckedChange = {
                        ViewModel.updatePhotoServerSettings(photoserverSettings.copy(photoserverEnabled = it))
                    },
                )
                if (photoserverSettings.photoserverEnabled) {
                    Text("Включен")
                } else {
                    Text("Выключен")
                }
            }
            IpConnectionTextField(
                address = photoserverAddress ?: "",
                onValueChange = {
                    photoserverAddress = it
                },
                checkConnection = {
                    withContext(Dispatchers.IO) {
                        val inetAddress =
                            InetAddress.getByAddress(photoserverAddress!!.split(".").map { it.toInt().toByte() }
                                .toByteArray())
                        val isReachable = inetAddress.isReachable(5000)
                        if (isReachable) photoserverInetAddress = inetAddress
                        isReachable
                    }
                },
                onConnectionSuccess = {
                    ViewModel.updatePhotoServerSettings(photoserverSettings.copy(photoserverAddress = photoserverInetAddress))
                },
                modifier = Modifier,
                label = { Text(text = "Адерес фотосервера") }
            )
        }
    }
}