package com.queatz.ailaai.trade

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.isNumericTextInput
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.item.InventoryItemDetails
import com.queatz.ailaai.item.upTo
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.InventoryItemExtended

@Composable
fun TradeItemDialog(
    onDismissRequest: () -> Unit,
    inventoryItem: InventoryItemExtended,
    initialQuantity: Double,
    maxQuantity: Double,
    isMine: Boolean,
    isAdd: Boolean = false,
    enabled: Boolean = true,
    confirmButton: String = stringResource(R.string.add),
    onQuantity: (quantity: Double) -> Unit
) {
    var quantity by rememberStateOf(
        if (inventoryItem.item?.divisible == true) {
            initialQuantity.toString()
        } else {
            initialQuantity.format()
        }
    )
    val focusRequester = remember {
        FocusRequester()
    }

    LaunchedEffect(isAdd) {
        if (isAdd) {
            focusRequester.requestFocus()
        }
    }

    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            content = {
                InventoryItemDetails(inventoryItem)
                if (isMine) {
                    OutlinedTextField(
                        value = quantity,
                        shape = MaterialTheme.shapes.large,
                        onValueChange = {
                            if (it.isNumericTextInput(allowDecimal = inventoryItem.item?.divisible == true)) {
                                quantity = it.upTo(maxQuantity)
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
                        enabled = enabled,
                        modifier = Modifier
                            .padding(vertical = 1.pad)
                            .focusRequester(focusRequester)
                            .fillMaxWidth()
                    )
                }
            },
            actions = {
                val quantity = quantity.toDoubleOrNull() ?: 0.0
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
                if (isAdd) {
                    Button(
                        {
                            onQuantity(quantity)
                        },
                        enabled = enabled && quantity > 0.0
                    ) {
                        Text(confirmButton)
                    }
                } else if (isMine) {
                    Button(
                        {
                            onQuantity(quantity)
                        },
                        enabled = enabled
                    ) {
                        Text(
                            if (quantity > 0.0) {
                                stringResource(R.string.update)
                            } else {
                                stringResource(R.string.remove)
                            }
                        )
                    }
                }
            }
        )
    }
}
