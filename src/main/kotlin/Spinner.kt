import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

interface Spinnable {

    override fun toString(): String

}

@Composable
fun Spinner(
    data: List<Spinnable>,
    value: String = "",
    onSelectedChanges: (Spinnable) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
    label: @Composable () -> Unit = {},
    Content: @Composable (Spinnable) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    var searchText by remember(value) { mutableStateOf(value) }

    var filter by remember { mutableStateOf(false) }

    Row {
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                filter = true
                expanded = true
            },
            label = label,
            isError = isError,
            textStyle = MaterialTheme.typography.h6,
            modifier = Modifier.onFocusChanged { expanded = it.isFocused }.then(modifier),
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }
        )
        SpinnerDropdown(
            expanded = expanded,
            onExpandedChanges = { expanded = it },
            data = if (filter) data.filter { element ->
                element.toString().contains(searchText, ignoreCase = true)
            } else data,
            onSelectedChanges = {
                searchText = it.toString()
                onSelectedChanges(it)
            },
            Content = Content
        )
    }
}

@Composable
fun SpinnerDropdown(
    expanded: Boolean,
    onExpandedChanges: (Boolean) -> Unit,
    data: List<Spinnable>,
    onSelectedChanges: (Spinnable) -> Unit,
    Content: @Composable (Spinnable) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onExpandedChanges(false) },
        focusable = false,
        modifier = Modifier.requiredSizeIn(maxHeight = 200.dp),
    ) {
        data.forEach {
            DropdownMenuItem(
                onClick = {
                    onExpandedChanges(false)
                    onSelectedChanges(it)
                }
            ) {
                Content(it)
            }
        }
    }
}

@Preview
@Composable
fun SpinnerPreview() {
    MaterialTheme {
        Spinner(
            List(5) {
                object : Spinnable {
                    override fun toString() = "Test $it"
                }
            },
            "Test 1",
            onSelectedChanges = {},
            label = {}
        ) {
            Text(text = it.toString())
        }
    }
}