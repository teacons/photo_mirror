package layoutEditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.*

@Composable
fun LayersList(
    layersList: List<Layer>,
    onLayersOrderChanged: (ItemPosition, ItemPosition) -> Unit,
    onClick: (Layer) -> Unit,
    onDelete: (Layer) -> Unit
) {
    val state = rememberReorderState()

    LazyColumn(
        state = state.listState,
        modifier = Modifier.reorderable(
            state = state, onMove = onLayersOrderChanged
        )
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp),
    ) {
        itemsIndexed(layersList) { idx, item ->
            Card(
                modifier = Modifier
                    .clickable { onClick(item) }
                    .draggedItem(state.offsetByIndex(idx))
                    .detectReorder(state)
                    .fillParentMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.weight(1f)

                    )
                    IconButton(
                        onClick = {onDelete(item)}
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}