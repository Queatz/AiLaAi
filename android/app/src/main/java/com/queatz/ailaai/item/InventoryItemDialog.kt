package com.queatz.ailaai.item

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.isExpired
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.db.InventoryItemExtended

@Composable
fun InventoryItemDialog(
    onDismissRequest: () -> Unit,
    inventoryItem: InventoryItemExtended,
    onDrop: () -> Unit,
    onTrade: () -> Unit,
) {
    val expired = inventoryItem.inventoryItem!!.isExpired

    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            content = {
                InventoryItemDetails(inventoryItem)
            },
            actions = {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
                TextButton(
                    {
                        onDrop()
                    }
                ) {
                    Text(stringResource(R.string.drop))
                }
                Button(
                    {
                        onTrade()
                    },
                    enabled = !expired
                ) {
                    Text(stringResource(R.string.trade))
                }
            }
        )
    }
}
