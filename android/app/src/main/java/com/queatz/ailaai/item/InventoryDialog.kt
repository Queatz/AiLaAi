package com.queatz.ailaai.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.InventoryItemExtended

@Composable
fun InventoryDialog(
    onDismissRequest: () -> Unit,
    items: List<InventoryItemExtended>,
    title: String = stringResource(R.string.treasure_chest),
    onInventoryItem: (InventoryItemExtended) -> Unit
) {
    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = false,
            content = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(bottom = 1.pad)
                    )
                }
                if (items.isEmpty()) {
                    EmptyText(stringResource(R.string.no_items))
                } else {
                    InventoryItems(
                        items = items,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        onInventoryItem(it)
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
            }
        )
    }
}
