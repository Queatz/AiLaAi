package com.queatz.ailaai.item

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.formatItemQuantity
import com.queatz.ailaai.extensions.isExpired
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toItemQuantity
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.InventoryItemExtended

@Composable
fun InventoryItemDialog(
    onDismissRequest: () -> Unit,
    inventoryItem: InventoryItemExtended,
    initialQuantity: Double = 1.0,
    onDrop: (quantity: Double) -> Unit,
    onEquip: (quantity: Double) -> Unit,
    onUnequip: (quantity: Double) -> Unit,
    onTrade: (quantity: Double) -> Unit
) {
    val expired = inventoryItem.inventoryItem!!.isExpired
    var quantity by rememberStateOf(
        initialQuantity.coerceAtMost(inventoryItem.inventoryItem!!.quantity!!).formatItemQuantity()
    )
    val focusRequester = remember {
        FocusRequester()
    }
    val enabled = quantity.toItemQuantity()?.let { it > 0.0 } == true

    LaunchedEffect(inventoryItem) {
        quantity = quantity.upTo(inventoryItem.inventoryItem!!.quantity!!)
    }

    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            content = {
                InventoryItemDetails(inventoryItem)
                OutlinedTextField(
                    value = quantity,
                    shape = MaterialTheme.shapes.large,
                    onValueChange = {
                        quantity = it.upTo(inventoryItem.inventoryItem!!.quantity!!)
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                quantity = (inventoryItem.inventoryItem?.quantity ?: 0.0).formatItemQuantity()
                            },
                            enabled = quantity != (inventoryItem.inventoryItem?.quantity ?: 0.0).formatItemQuantity(),
                            modifier = Modifier
                                .padding(end = 1.pad)
                        ) {
                            Text(
                                text = stringResource(R.string.all),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    label = {
                        Text(
                            stringResource(R.string.quantity)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (inventoryItem.item?.divisible == true) {
                            KeyboardType.Decimal
                        } else {
                            KeyboardType.Number
                        }
                    ),
                    modifier = Modifier
                        .padding(vertical = 1.pad)
                        .focusRequester(focusRequester)
                        .fillMaxWidth()
                )
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
                        quantity.toItemQuantity()?.let(onDrop)
                    },
                    enabled = enabled
                ) {
                    Text(stringResource(R.string.drop))
                }
                if (inventoryItem.inventoryItem?.equipped == true) {
                    TextButton(
                        {
                            quantity.toItemQuantity()?.let(onUnequip)
                        },
                        enabled = enabled
                    ) {
                        Text(stringResource(R.string.unequip))
                    }
                } else {
                    TextButton(
                        {
                            quantity.toItemQuantity()?.let(onEquip)
                        },
                        enabled = enabled
                    ) {
                        Text(stringResource(R.string.equip))
                    }
                }
                Button(
                    {
                        quantity.toItemQuantity()?.let(onTrade)
                    },
                    enabled = !expired && enabled
                ) {
                    Text(stringResource(R.string.trade))
                }
            }
        )
    }
}
