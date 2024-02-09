package com.queatz.ailaai.trade

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cancelTrade
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.item.InventoryItemLayout
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.*
import confirmTrade
import kotlinx.coroutines.launch
import trade
import unconfirmTrade
import updateTradeItems

data class TradeMemberItem(
    val inventoryItem: InventoryItemExtended,
    val from: Person,
    val to: Person,
    val quantity: Double
)

data class TradeItemMember(
    val item: TradeItem,
    val from: TradeMember
)

data class TradeMemberState(
    val person: Person,
    val items: List<TradeMemberItem>,
    val confirmed: Boolean
)

@Composable
fun TradeDialog(
    onDismissRequest: () -> Unit,
    tradeId: String
) {
    val scope = rememberCoroutineScope()
    var trade by rememberStateOf<TradeExtended?>(null)
    var isLoading by rememberStateOf(false)
    var showCancelDialog by rememberStateOf(false)
    var editItemDialog by rememberStateOf<TradeMemberItem?>(null)
    var addItemDialog by rememberStateOf<TradeMemberItem?>(null)
    var addInventoryItemDialog by rememberStateOf<Person?>(null)
    val me = me ?: return
    val context = LocalContext.current

    val anyConfirmed = trade?.trade?.members?.any { it.confirmed == true } == true
    val confirmedByMe = trade?.trade?.members?.any { it.person == me.id!! && it.confirmed == true } == true
    val myTradeMember = trade?.trade?.members?.first { it.person == me.id }
    val enableConfirm = confirmedByMe || trade?.trade?.members?.any { it.items!!.isNotEmpty() } == true

    // todo reload on trade updates, cancelled, completed pushes

    val members = remember(trade) {
        trade?.let { trade ->
            val allItems = trade.trade!!.members!!
                .flatMap { member -> member.items!!.map { TradeItemMember(it, member) } }
                .map { item ->
                    TradeMemberItem(
                        inventoryItem = trade.inventoryItems!!.first { it.inventoryItem!!.id == item.item.inventoryItem!! },
                        from = trade.people!!.first { it.id == item.from.person },
                        to = trade.people!!.first { it.id == item.item.to },
                        quantity = item.item.quantity!!
                    )
                }
            trade.trade!!.members!!.map { member ->
                TradeMemberState(
                    person = trade.people!!.first { it.id == member.person },
                    items = allItems.filter {
                        it.to.id == member.person
                    },
                    confirmed = member.confirmed == true
                )
            }
        } ?: emptyList()
    }

    fun reload() {
        scope.launch {
            api.trade(tradeId) {
                trade = it
            }
        }
    }

    fun cancel() {
        scope.launch {
            api.cancelTrade(tradeId) {
                context.toast(R.string.trade_cancelled)
                onDismissRequest()
            }
        }
    }

    fun confirmUnconfirm() {
        if (confirmedByMe) {
            scope.launch {
                api.unconfirmTrade(tradeId) {
                    trade = it
                }
            }
        } else {
            scope.launch {
                api.confirmTrade(tradeId, trade!!.trade!!) {
                    trade = it
                }
            }
        }
    }

    LaunchedEffect(tradeId) {
        trade = null
        isLoading = true
        reload()
        isLoading = false
    }

    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            scrollable = false,
            content = {
                Text(
                    stringResource(R.string.trade),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(bottom = 1.pad)
                )
                if (isLoading) {
                    Loading()
                } else {
                    trade?.let { trade ->
                        LazyVerticalGrid(
                            state = rememberLazyGridState(),
                            columns = GridCells.Adaptive(96.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.pad),
                            verticalArrangement = Arrangement.spacedBy(1.pad),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(bottom = 1.pad)
                        ) {
                            members.forEach { member ->
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(1.pad)
                                    ) {
                                        Text(
                                            member.person.name ?: stringResource(R.string.someone)
                                        )
                                        if (member.confirmed) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                stringResource(R.string.confirmed),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                if ((member.person.id == me.id || anyConfirmed) && member.items.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        OutlinedCard(
                                            shape = MaterialTheme.shapes.large,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            EmptyText(stringResource(R.string.no_items))
                                        }
                                    }
                                } else {
                                    items(member.items, key = { it.inventoryItem.inventoryItem!!.id!! }) { item ->
                                        InventoryItemLayout(item.inventoryItem, quantity = item.quantity) {
                                            editItemDialog = item
                                        }
                                    }

                                    if (member.person.id != me.id && !anyConfirmed) {
                                        item {
                                            AddInventoryItemButton {
                                                addInventoryItemDialog = member.person
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
                        showCancelDialog = true
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                if (confirmedByMe) {
                    OutlinedButton(
                        {
                            confirmUnconfirm()
                        },
                        enabled = enableConfirm
                    ) {
                        Text(
                            stringResource(
                                R.string.unconfirm
                            )
                        )
                    }
                } else {
                    Button(
                        {
                            confirmUnconfirm()
                        },
                        enabled = enableConfirm
                    ) {
                        Text(
                            stringResource(
                                R.string.confirm
                            )
                        )
                    }
                }
            }
        )
    }

    addInventoryItemDialog?.let { person ->
        AddInventoryItemDialog(
            {
                addInventoryItemDialog = null
            },
            omit = myTradeMember?.items?.map { it.inventoryItem!! } ?: emptyList()
        ) { item ->
            val quantity = 1.0.coerceAtMost(item.inventoryItem!!.quantity!!)

            addItemDialog = TradeMemberItem(
                item,
                me,
                person,
                quantity
            )
            addInventoryItemDialog = null
        }
    }

    editItemDialog?.let { item ->
        TradeItemDialog(
            {
                editItemDialog = null
            },
            item.inventoryItem,
            initialQuantity = item.quantity,
            isMine = item.from.id == me.id,
            enabled = !anyConfirmed,
            onQuantity = { newQuantity ->
                val items = myTradeMember!!.items!!.mapNotNull {
                    if (it.inventoryItem == editItemDialog!!.inventoryItem.inventoryItem!!.id!!) {
                        if (newQuantity > 0.0) {
                            it.copy(quantity = newQuantity)
                        } else {
                            null
                        }
                    } else {
                        it
                    }
                }
                editItemDialog = null
                scope.launch {
                    api.updateTradeItems(tradeId, items) {
                        trade = it
                    }
                }
            }
        )
    }

    addItemDialog?.let { item ->
        TradeItemDialog(
            {
                addItemDialog = null
            },
            item.inventoryItem,
            initialQuantity = item.quantity,
            isAdd = true,
            isMine = item.from.id == me.id,
            enabled = !anyConfirmed,
            onQuantity = { newQuantity ->
                if (newQuantity > 0.0) {
                    val items = (myTradeMember!!.items!! + TradeItem(
                        inventoryItem = item.inventoryItem.inventoryItem!!.id!!,
                        quantity = newQuantity,
                        to = item.to.id!!
                    )).distinctBy { it.inventoryItem!! }
                    scope.launch {
                        api.updateTradeItems(tradeId, items) {
                            trade = it
                        }
                    }
                }
                addItemDialog = null
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            {
                showCancelDialog = false
            },
            title = {
                Text(stringResource(R.string.cancel_this_trade))
            },
            confirmButton = {
                TextButton(
                    {
                        cancel()
                        showCancelDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel_trade))
                }
            },
            dismissButton = {
                TextButton(
                    {
                        showCancelDialog = false
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
