package com.queatz.ailaai.ui.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.item.InventoryDialog
import com.queatz.ailaai.trade.TradeItemDialog
import com.queatz.db.Inventory
import com.queatz.db.InventoryItemExtended
import com.queatz.db.TakeInventoryItem
import inventoriesNear
import inventory
import kotlinx.coroutines.launch
import takeInventory

data class MapInventoryControl(
    val reloadInventories: suspend () -> Unit,
    val inventories: List<Inventory>,
    val showInventory: (String) -> Unit
)

@Composable
fun mapInventoryControl(
    geo: LatLng?
): MapInventoryControl {
    val scope = rememberCoroutineScope()
    var inventories by rememberStateOf(emptyList<Inventory>())
    var showInventory by rememberStateOf<String?>(null)
    var showInventoryDialog by rememberStateOf<List<InventoryItemExtended>?>(null)
    var showInventoryItemDialog by rememberStateOf<InventoryItemExtended?>(null)

    suspend fun reloadInventories() {
        if (geo != null) {
            api.inventoriesNear(geo.toGeo()) {
                inventories = it
            }
        } else {
            inventories = emptyList()
        }
    }

    LaunchedEffect(geo) {
        reloadInventories()
    }

    LaunchedEffect(showInventory) {
        showInventory?.let {
            api.inventory(it) {
                showInventoryDialog = it
            }
        }
    }

    showInventoryDialog?.let { items ->
        InventoryDialog(
            onDismissRequest = {
                showInventory = null
                showInventoryDialog = null
            },
            items = items
        ) {
            showInventoryItemDialog = it
        }
    }

    showInventoryItemDialog?.let { item ->
        val quantity = item.inventoryItem?.quantity ?: 0.0
        TradeItemDialog(
            onDismissRequest = {
                showInventoryItemDialog = null
            },
            inventoryItem = item,
            initialQuantity = quantity,
            maxQuantity = quantity,
            isAdd = true,
            isMine = true,
            enabled = true,
            confirmButton = stringResource(R.string.pick_up),
            onQuantity = { quantity ->
                scope.launch {
                    api.takeInventory(
                        item.inventoryItem!!.inventory!!,
                        listOf(TakeInventoryItem(item.inventoryItem!!.id!!, quantity))
                    ) {
                        showInventory?.let {
                            api.inventory(it) {
                                if (it.isEmpty()) {
                                    showInventory = null
                                    showInventoryDialog = null
                                } else {
                                    showInventoryDialog = it
                                }
                            }
                        }
                        showInventoryItemDialog = null
                        reloadInventories()
                    }
                }
            }
        )
    }

    return MapInventoryControl(
        reloadInventories = { reloadInventories() },
        inventories = inventories,
        showInventory = {
            showInventory = it
        }
    )
}
