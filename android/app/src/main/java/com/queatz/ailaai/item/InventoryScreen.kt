package com.queatz.ailaai.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.trade.TradeDialog
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.*
import createTrade
import dropItem
import kotlinx.coroutines.launch
import myInventory

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
    var showDropInventoryItem by rememberStateOf<InventoryItemExtended?>(null)
    var showStartTradeDialog by rememberStateOf<InventoryItemExtended?>(null)
    var inventory by rememberStateOf<List<InventoryItemExtended>>(emptyList())
    var showTradeDialog by rememberStateOf<Trade?>(null)

    fun scrollToTop() {
        scope.launch {
            state.scrollToTop()
        }
    }

    fun drop(inventoryItem: InventoryItem, quantity: Double) {
        scope.launch {
            api.dropItem(inventoryItem.id!!, DropItemBody(quantity))
            context.toast(R.string.items_dropped)
        }
    }

    fun tradeWith(people: List<Person>) {
        scope.launch {
            api.createTrade(
                Trade().apply {
                    this.people = (people.map { it.id!! } + me!!.id!!).distinct()
                }
            ) {
                showTradeDialog = it
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        api.myInventory {
            inventory = it
        }
        isLoading = false
    }

    showTradeDialog?.let {
        TradeDialog(
            {
                showTradeDialog = null
            },
            it.id!!
        )
    }

    if (showInventoryItem != null) {
        InventoryItemDialog(
            {
                showInventoryItem = null
            },
            showInventoryItem!!,
            onDrop = {
                showDropInventoryItem = showInventoryItem
                showInventoryItem = null
            }
        ) {
            showStartTradeDialog = showInventoryItem
            showInventoryItem = null
        }
    }

    if (showDropInventoryItem != null) {
        TextFieldDialog(
            onDismissRequest = {
                showDropInventoryItem = null
            },
            title = showDropInventoryItem?.item?.name,
            button = stringResource(R.string.drop),
            placeholder = stringResource(R.string.quantity),
            initialValue = if (showDropInventoryItem?.item?.divisible == true) {
                showDropInventoryItem!!.inventoryItem!!.quantity!!.toString()
            } else {
                showDropInventoryItem!!.inventoryItem!!.quantity!!.format()
            },
            requireNotBlank = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (showDropInventoryItem?.item?.divisible == true) {
                    KeyboardType.Decimal
                } else {
                    KeyboardType.Number
                }
            ),
            valueFormatter = {
                if (showDropInventoryItem?.item?.divisible == true) {
                    if (it.isNumericTextInput()) it else null
                } else {
                    if (it.isNumericTextInput(allowDecimal = false)) it else null
                }
            }
        ) {
            it.toDoubleOrNull()?.let {
                drop(showDropInventoryItem!!.inventoryItem!!, it)
                showDropInventoryItem = null
            } ?: context.showDidntWork()
        }
    }

    if (showStartTradeDialog != null) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            {
                showStartTradeDialog = null
            },
            title = stringResource(R.string.trade),
            confirmFormatter = defaultConfirmFormatter(
                R.string.trade,
                R.string.trade_with_x,
                R.string.trade_with_x_and_x,
                R.string.trade_with_x_people
            ) { it.name ?: someone },
            omit = { it.id == me?.id },
            multiple = true,
            onPeopleSelected = {
                tradeWith(it)
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
                // actions
            }
            if (isLoading) {
                Loading()
            } else {
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
