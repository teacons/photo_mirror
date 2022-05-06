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
import androidx.compose.ui.unit.dp

interface Spinnable {

    override fun toString(): String

}

@Composable
fun Spinner(data: List<Spinnable>, selected: Spinnable, onSelectedChanges: (Spinnable) -> Unit) {

    var expanded by remember { mutableStateOf(false) }

    Card(
        backgroundColor = MaterialTheme.colors.background,
        border = BorderStroke(1.dp, MaterialTheme.colors.primary),
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { expanded = !expanded }

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
                        Text(text = it.toString())
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
        Spinner(List(5) { CameraHelper("Test $it", null) }, CameraHelper("Test 1", null)) {}
    }
}