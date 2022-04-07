import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Spinner(data: List<String>, selected: String, onSelectedChanges: (String) -> Unit) {

    var expanded by remember { mutableStateOf(false) }

    Card(
        backgroundColor = MaterialTheme.colors.background,
        border = BorderStroke(1.dp, Color.Black),
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { expanded = !expanded }

    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = selected,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = null
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                data.forEach {
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onSelectedChanges(it)
                        }
                    ) {
                        Text(text = it)
                    }
                }
            }

        }
    }
}

@Preview
@Composable
fun SpinnerPreview() {
    MaterialTheme {
        Spinner(List(5) { "Test $it" }, "TEST SELECT") {}
    }
}