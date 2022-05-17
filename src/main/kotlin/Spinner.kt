import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

interface Spinnable {

    override fun toString(): String

}

@Composable
fun Spinner(
    data: List<Spinnable>,
    selected: Spinnable,
    onSelectedChanges: (Spinnable) -> Unit,
    modifier: Modifier = Modifier,
    Content: @Composable (Spinnable) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    Card(
        backgroundColor = MaterialTheme.colors.background,
        border = BorderStroke(1.dp, MaterialTheme.colors.primary),
        modifier = Modifier
            .clickable { expanded = !expanded }.then(modifier)

    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = selected.toString(),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = null,
                tint = MaterialTheme.colors.primary
            )
            SpinnerDropdown(
                expanded = expanded,
                onExpandedChanges = { expanded = it },
                data = data,
                onSelectedChanges = onSelectedChanges,
                Content = Content
            )
        }
    }
}

@Composable
fun SearchableSpinner(
    data: List<Spinnable>,
    onSelectedChanges: (Spinnable) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    Content: @Composable (Spinnable) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    var searchText by remember { mutableStateOf("") }

    var searchedData by remember { mutableStateOf(data) }

    Box(modifier = modifier) {
        Row {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    searchedData = data
                        .filter { element -> element.toString().contains(searchText, ignoreCase = true) }
                    expanded = true
                },
                label = label,
                isError = isError,
                textStyle = MaterialTheme.typography.h6,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            )
            SpinnerDropdown(
                expanded = expanded,
                onExpandedChanges = { expanded = it },
                data = searchedData,
                onSelectedChanges = {
                    searchText = it.toString()
                    onSelectedChanges(it)
                },
                Content = Content
            )
        }
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
            List(5) { CameraHelper("Test $it", null) },
            CameraHelper("Test 1", null),
            onSelectedChanges = {}
        ) {
            Text(text = it.toString())
        }
    }
}