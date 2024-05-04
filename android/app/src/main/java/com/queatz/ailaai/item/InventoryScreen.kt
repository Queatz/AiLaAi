package com.queatz.ailaai.item

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.SwipeResult
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.swipe
import com.queatz.ailaai.extensions.toList
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.trading
import com.queatz.ailaai.trade.TradeDialog
import com.queatz.ailaai.trade.TradesDialog
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.ScanQrCodeButton
import com.queatz.ailaai.ui.components.ScanQrCodeResult
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.components.swipeMainTabs
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.SetLocationDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.DropItemBody
import com.queatz.db.EquipItemBody
import com.queatz.db.InventoryItem
import com.queatz.db.InventoryItemExtended
import com.queatz.db.Trade
import com.queatz.db.TradeExtended
import com.queatz.db.TradeItem
import com.queatz.db.UnequipItemBody
import completedTrades
import createTrade
import dropItem
import equipItem
import kotlinx.coroutines.launch
import myInventory
import trades
import unequipItem
import updateTradeItems

@Composable
fun InventoryScreen() {
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val nav = nav
    val me = me

    var search by rememberSavableStateOf("")
    var isLoading by rememberStateOf(false)
    var showInventoryItem by rememberStateOf<InventoryItemExtended?>(null)
    var showDropInventoryItem by rememberStateOf<Pair<InventoryItemExtended, Double>?>(null)
    var showStartTradeDialog by rememberStateOf(false)
    var showActiveTradesDialog by rememberStateOf(false)
    var showCompletedTradesDialog by rememberStateOf(false)
    var showMenu by rememberStateOf(false)
    var showDropMenu by rememberStateOf(false)
    var showStartTradeDialogItem by rememberStateOf<Pair<InventoryItemExtended, Double>?>(null)
    var inventory by rememberStateOf<List<InventoryItemExtended>>(emptyList())
    var shownInventory by rememberStateOf<List<InventoryItemExtended>>(emptyList())
    var showTradeDialog by rememberStateOf<Trade?>(null)
    val activeTrades by trading.activeTrades.collectAsState()
    var completedTrades by rememberStateOf<List<TradeExtended>?>(null)
    var geo by rememberStateOf<LatLng?>(null)

    LaunchedEffect(inventory, search) {
        shownInventory = if (search.isBlank()) inventory else inventory.filter {
            it.item!!.name!!.contains(search, ignoreCase = true) ||
                    it.item!!.description!!.contains(search, ignoreCase = true)
        }
    }

    fun scrollToTop() {
        scope.launch {
            state.scrollToTop()
        }
    }

    suspend fun reload() {
        api.myInventory {
            inventory = it

            if (showInventoryItem != null) {
                showInventoryItem =
                    inventory.firstOrNull { showInventoryItem?.inventoryItem?.id == it.inventoryItem?.id }
            }
        }
    }

    fun drop(inventoryItem: InventoryItem, quantity: Double, geo: LatLng? = null) {
        scope.launch {
            api.dropItem(inventoryItem.id!!, DropItemBody(quantity, geo?.toList()))
            reload()
            context.toast(R.string.items_dropped)
        }
    }

    fun equip(inventoryItem: InventoryItem, quantity: Double) {
        scope.launch {
            api.equipItem(inventoryItem.id!!, EquipItemBody(quantity))
            reload()
            context.toast(R.string.items_equipped)
        }
    }

    fun unequip(inventoryItem: InventoryItem, quantity: Double) {
        scope.launch {
            api.unequipItem(inventoryItem.id!!, UnequipItemBody(quantity))
            reload()
            context.toast(R.string.items_unequipped)
        }
    }

    fun tradeWith(
        people: List<String> = emptyList(),
        items: List<Pair<InventoryItemExtended, Double>> = emptyList()
    ) {
        scope.launch {
            api.createTrade(
                Trade().apply {
                    this.people = (listOf(me!!.id!!) + people).distinct()
                }
            ) {
                if (items.isEmpty()) {
                    showTradeDialog = it
                } else {
                    api.updateTradeItems(
                        it.id!!,
                        items.map {
                            TradeItem(
                                inventoryItem = it.first.inventoryItem!!.id!!,
                                quantity = it.second,
                                to = people.first()
                            )
                        }
                    ) {
                        showTradeDialog = it.trade
                    }
                }
            }
            trading.reload()
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        reload()
        isLoading = false
    }

    ResumeEffect {
        trading.reload()
    }

    showTradeDialog?.let {
        TradeDialog(
            {
                showTradeDialog = null
            },
            it.id!!,
            onTradeCancelled = {
                scope.launch {
                    trading.reload()
                }
            },
            onTradeCompleted = {
                scope.launch {
                    trading.reload()
                }
            }
        )
    }

    if (showActiveTradesDialog) {
        TradesDialog(
            {
                showActiveTradesDialog = false
            },
            activeTrades
        ) {
            showTradeDialog = it.trade
            showActiveTradesDialog = false
        }
    }

    if (showCompletedTradesDialog) {
        LaunchedEffect(Unit) {
            api.completedTrades {
                completedTrades = it
            }
        }

        if (completedTrades != null) {
            TradesDialog(
                {
                    showCompletedTradesDialog = false
                },
                completedTrades!!
            ) {
                showTradeDialog = it.trade
                showCompletedTradesDialog = false
            }
        }
    }

    if (showInventoryItem != null) {
        InventoryItemDialog(
            {
                showInventoryItem = null
            },
            showInventoryItem!!,
            onDrop = {
                showDropInventoryItem = showInventoryItem!! to it
            },
            onEquip = {
                equip(showInventoryItem!!.inventoryItem!!, it)
            },
            onUnequip = {
                unequip(showInventoryItem!!.inventoryItem!!, it)
            }
        ) {
            showStartTradeDialog = true
            showStartTradeDialogItem = showInventoryItem!! to it
            showInventoryItem = null
        }
    }

    if (showDropInventoryItem != null) {
        SetLocationDialog(
            {
                showDropInventoryItem = null
            },
            confirmButton = stringResource(R.string.drop),
            initialLocation = geo ?: LatLng(0.0, 0.0),
            initialZoom = 18f,
            title = pluralStringResource(
                R.plurals.drop_x_items,
                showDropInventoryItem!!.second.toInt(),
                showDropInventoryItem!!.second
            ),
            actions = {
                IconButton(
                    {
                        showDropMenu = true
                    }
                ) {
                    Icon(Icons.Outlined.MoreVert, null)

                    Dropdown(showDropMenu, { showDropMenu = false }) {
                        DropdownMenuItem({
                            Text(stringResource(R.string.discard_items))
                        }, {
                            showDropMenu = false
                            showDropInventoryItem?.let { drop ->
                                drop(drop.first.inventoryItem!!, drop.second, null)
                            }
                            showDropInventoryItem = null
                            scope.launch {
                                reload()
                            }
                        })
                    }
                }
            }
        ) { geo ->
            showDropInventoryItem?.let { drop ->
                drop.second.let {
                    drop(drop.first.inventoryItem!!, it, geo)
                    showDropInventoryItem = null
                    scope.launch {
                        reload()
                    }
                }
            }
        }
    }

    if (showStartTradeDialog) {
        val someone = stringResource(R.string.someone)
        val item = showStartTradeDialogItem
        ChoosePeopleDialog(
            {
                showStartTradeDialog = false
                showStartTradeDialogItem = null
            },
            title = stringResource(R.string.trade),
            confirmFormatter = defaultConfirmFormatter(
                R.string.trade,
                R.string.trade_with_x,
                R.string.trade_with_x_and_x,
                R.string.trade_with_x_people
            ) { it.name ?: someone },
            omit = { it.id == me?.id },
            multiple = showStartTradeDialogItem == null,
            onPeopleSelected = {
                tradeWith(it.map { it.id!! }, item?.inList() ?: emptyList())
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeMainTabs {
                when (emptyList<Unit>().swipe(Unit, it)) {
                    is SwipeResult.Previous -> {
                        nav.navigate(AppNav.Explore)
                    }

                    is SwipeResult.Next -> {
                        nav.navigate(AppNav.Stories)
                    }

                    is SwipeResult.Select<*> -> {
                        // Impossible
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                stringResource(R.string.items),
                {
                    scrollToTop()
                },
            ) {
                IconButton(
                    {
                        showMenu = true
                    }
                ) {
                    Icon(Icons.Outlined.MoreVert, null)
                    Dropdown(showMenu, { showMenu = false }) {
                        DropdownMenuItem(
                            {
                                Text(stringResource(R.string.history))
                            },
                            onClick = {
                                showCompletedTradesDialog = true
                            }
                        )
                    }
                }
                IconButton(
                    {
                        showStartTradeDialogItem = null
                        showStartTradeDialog = true
                    }
                ) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.trade))
                }
                ScanQrCodeButton {
                    when (it) {
                        is ScanQrCodeResult.Profile -> {
                            tradeWith(listOf(it.id))
                        }

                        else -> context.showDidntWork()
                    }
                }
            }
            if (isLoading) {
                Loading()
            } else {
                AnimatedVisibility(activeTrades.isNotEmpty()) {
                    OutlinedCard(
                        onClick = {
                            showActiveTradesDialog = true
                        },
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.elevatedCardElevation(1.elevation),
                        modifier = Modifier
                            .padding(horizontal = 1.pad)
                            .fillMaxWidth()
                    ) {
                        Text(
                            pluralStringResource(R.plurals.x_active_trades, activeTrades.size, activeTrades.size),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(1.pad)
                                .fillMaxWidth()
                        )
                    }
                }

                if (shownInventory.isEmpty()) {
                    EmptyText(stringResource(R.string.no_items))
                } else {
                    InventoryItems(
                        state = state,
                        items = shownInventory,
                        bottomContentPadding = 80.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        showInventoryItem = it
                    }
                }
            }
        }
        PageInput(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            SearchFieldAndAction(
                search,
                { search = it },
                action = {
                    Icon(Icons.Outlined.Edit, "")
                }
            ) {
                nav.navigate(AppNav.Items)
            }
        }
    }
}

fun String.upTo(maximumValue: Double) = toDoubleOrNull()?.let {
    if (it > maximumValue) {
        maximumValue.format()
    } else {
        null
    }
} ?: this
