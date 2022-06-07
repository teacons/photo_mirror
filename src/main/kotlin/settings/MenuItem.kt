package settings

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


enum class MenuItem(val itemName: String) {
    PhotoCamera("Фотоаппарат"),
    Printer("Принтер"),
    PhotoServer("Фотосервер"),
    Layout("Макет"),
    GuestScreen("Экран работы")
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MenuItem(item: MenuItem, selectedItem: MenuItem, onMenuItemClick: () -> Unit) {
    ListItem(
        text = {
            Text(
                text = item.itemName,
                style = if (selectedItem == item) MaterialTheme.typography.h6.copy(fontWeight = FontWeight.ExtraBold)
                else MaterialTheme.typography.h6
            )
        },
        modifier = Modifier
            .background(if (selectedItem == item) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onMenuItemClick)
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    )
}

@Preview
@Composable
fun MenuItemPreview() {
    MaterialTheme {
        MenuItem(MenuItem.PhotoCamera, MenuItem.PhotoCamera) {}
    }
}