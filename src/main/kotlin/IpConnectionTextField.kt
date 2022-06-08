import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.validator.routines.InetAddressValidator

@Composable
fun IpConnectionTextField(
    address: String,
    onValueChange: (String) -> Unit,
    checkConnection: suspend (String) -> Boolean,
    onConnectionSuccess: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var trailingIcon by remember { mutableStateOf<@Composable () -> Unit>({}) }
    val inetAddressValidator by remember { mutableStateOf(InetAddressValidator.getInstance()) }
    val coroutineScope = rememberCoroutineScope()
    var connectionIsChecking by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = address,
            onValueChange = onValueChange,
            trailingIcon = trailingIcon,
            isError = !inetAddressValidator.isValid(address),
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            placeholder = { Text(text = "10.0.0.100") },
            label = label,
            modifier = modifier
        )
        Button(
            onClick = {
                if (connectionIsChecking) return@Button
                coroutineScope.launch {
                    connectionIsChecking = true
                    if (inetAddressValidator.isValid(address)) {
                        trailingIcon = { CircularProgressIndicator() }
                        if (checkConnection(address)) {
                            trailingIcon = { Icon(Icons.Filled.Done, null) }
                            delay(1000L)
                            onConnectionSuccess()
                        } else {
                            trailingIcon = { Icon(Icons.Filled.Close, null) }
                        }
                    }
                    connectionIsChecking = false
                }
            }
        ) {
            Text(text = "Check")
        }
    }
}