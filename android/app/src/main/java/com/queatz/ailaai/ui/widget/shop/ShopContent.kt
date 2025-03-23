package com.queatz.ailaai.ui.widget.shop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.equippedItems
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.item.ItemLayout
import com.queatz.ailaai.me
import com.queatz.ailaai.trade.TradeItemDialog
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.InventoryItemExtended
import com.queatz.db.Widget
import com.queatz.widgets.widgets.ShopData
import widget

fun LazyGridScope.ShopContent(widgetId: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val me = me
        val scope = rememberCoroutineScope()
        var showInventoryItemDialog by rememberStateOf<InventoryItemExtended?>(null)

        var widget by remember(widgetId) {
            mutableStateOf<Widget?>(null)
        }

        var data by remember(widgetId) {
            mutableStateOf<ShopData?>(null)
        }

        var isLoading by remember(widgetId) {
            mutableStateOf(true)
        }

        var items by remember {
            mutableStateOf(emptyList<InventoryItemExtended>())
        }

        LaunchedEffect(data) {
            api.equippedItems(me!!.id!!) {
                items = it
            }
        }

        LaunchedEffect(widgetId) {
            isLoading = true
            api.widget(widgetId) {
                it.data ?: return@widget
                widget = it
                data = json.decodeFromString<ShopData>(it.data!!)
            }
            isLoading = false
        }

        if (isLoading) {
            Loading(
                modifier = Modifier
                    .padding(vertical = 1.pad)
            )
        } else {
            DisableSelection {
                showInventoryItemDialog?.let { item ->
                    val quantity = item.inventoryItem?.quantity ?: 0.0
                    TradeItemDialog(
                        onDismissRequest = {
                            showInventoryItemDialog = null
                        },
                        inventoryItem = item,
                        initialQuantity = quantity,
                        maxQuantity = quantity,
                        isMine = false,
                        enabled = false,
                        confirmButton = stringResource(R.string.close),
                        onQuantity = {
                            showInventoryItemDialog = null
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items.forEach {
                        ItemLayout(
                            item = it.item!!,
                            hint = "90,000đ",
                            endContent = {
                                Box(
                                    contentAlignment = Alignment.CenterEnd,
                                    modifier = Modifier
                                ) {
                                    Button(
                                        onClick = {
                                            // todo buy item (add to trade)
                                        }
                                    ) {
                                        Text(stringResource(R.string.buy))
                                    }
                                }
                            },
                            onClick = {
                                showInventoryItemDialog = it
                            }
                        )
                    }
                }
            }
        }
    }
}
