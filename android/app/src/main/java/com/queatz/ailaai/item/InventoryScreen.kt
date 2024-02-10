package com.queatz.ailaai.item

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.trade.TradeDialog
import com.queatz.ailaai.trade.TradesDialog
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.*
import createTrade
import dropItem
import kotlinx.coroutines.launch
import myInventory
import trades
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
    var showStartTradeDialogItem by rememberStateOf<Pair<InventoryItemExtended, Double>?>(null)
    var inventory by rememberStateOf<List<InventoryItemExtended>>(emptyList())
    var showTradeDialog by rememberStateOf<Trade?>(null)
    var activeTrades by rememberStateOf<List<TradeExtended>?>(null)

    fun scrollToTop() {
        scope.launch {
            state.scrollToTop()
        }
    }

    suspend fun reload() {
        api.myInventory {
            inventory = it

            if (showInventoryItem != null) {
                showInventoryItem = inventory.firstOrNull { showInventoryItem?.inventoryItem?.id == it.inventoryItem?.id }
            }
        }
    }

    fun drop(inventoryItem: InventoryItem, quantity: Double) {
        scope.launch {
            api.dropItem(inventoryItem.id!!, DropItemBody(quantity))
            reload()
            context.toast(R.string.items_dropped)
        }
    }

    fun tradeWith(people: List<String> = emptyList(), items: List<Pair<InventoryItemExtended, Double>> = emptyList()) {
        scope.launch {
            api.createTrade(
                Trade().apply {
                    this.people = (listOf(me!!.id!!) + people).distinct()
                }
            ) {
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
    }

    LaunchedEffect(Unit) {
        isLoading = true
        reload()
        isLoading = false
    }

    ResumeEffect {
        api.trades {
            activeTrades = it
        }
    }

    showTradeDialog?.let {
        TradeDialog(
            {
                showTradeDialog = null
            },
            it.id!!
        )
    }

    if (showActiveTradesDialog) {
        activeTrades?.let {
            TradesDialog(
                {
                    showActiveTradesDialog = false
                },
                it
            ) {
                showTradeDialog = it.trade
                showActiveTradesDialog = false
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
            }
        ) {
            showStartTradeDialog = true
            showStartTradeDialogItem = showInventoryItem!! to it
            showInventoryItem = null
        }
    }

    if (showDropInventoryItem != null) {
        AlertDialog(
            {
                showDropInventoryItem = null
            },
            title = {
                Text(stringResource(R.string.drop_x, showDropInventoryItem!!.second.format()))
            },
            confirmButton = {
                Button(
                    {
                        showDropInventoryItem?.let { drop ->
                            drop.second.let {
                                drop(drop.first.inventoryItem!!, it)
                                showDropInventoryItem = null
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.drop))
                }
            },
            dismissButton = {
                TextButton(
                    {
                        showDropInventoryItem = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                        nav.navigate("schedule")
                    }

                    is SwipeResult.Next -> {
                        nav.navigate("explore")
                    }

                    is SwipeResult.Select<*> -> {
                        // Impossible
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                stringResource(R.string.inventory),
                {
                    scrollToTop()
                },
            ) {
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
                AnimatedVisibility(activeTrades?.isNotEmpty() == true) {
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
                            pluralStringResource(R.plurals.x_active_trades, activeTrades!!.size, activeTrades!!.size),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(1.pad)
                                .fillMaxWidth()
                        )
                    }
                }

                LazyVerticalGrid(
                    state = state,
                    columns = GridCells.Adaptive(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.pad),
                    verticalArrangement = Arrangement.spacedBy(1.pad),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 1.pad,
                        end = 1.pad,
                        top = 1.pad,
                        bottom = 1.pad + 80.dp
                    )
                ) {
                    items(inventory, key = { it.inventoryItem!!.id!! }) {
                        InventoryItemLayout(it) {
                            showInventoryItem = it
                        }
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
                nav.navigate("items")
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
