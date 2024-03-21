package com.queatz.ailaai.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.InventoryItemExtended

@Composable
fun InventoryItems(
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    items: List<InventoryItemExtended>,
    onItem: (item: InventoryItemExtended) -> Unit
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(96.dp),
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 1.pad,
            end = 1.pad,
            top = 1.pad,
            bottom = 1.pad + 80.dp
        )
    ) {
        items(items, key = { it.inventoryItem!!.id!! }) {
            InventoryItemLayout(it) {
                onItem(it)
            }
        }
    }
}
