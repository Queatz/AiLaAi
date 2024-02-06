package com.queatz.ailaai.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ailaai.api.updateCard
import coil.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.InventoryItem
import com.queatz.db.InventoryItemExtended
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import myInventory
import kotlin.time.Duration.Companion.seconds

@Composable
fun InventoryScreen() {
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val nav = nav
    val me = me

    var search by rememberSavableStateOf("")
    var isLoading by rememberStateOf(false)
    var showInventoryItem by rememberStateOf<InventoryItemExtended?>(null)
    var showTradeDialog by rememberStateOf<InventoryItemExtended?>(null)
    var inventory by rememberStateOf<List<InventoryItemExtended>>(emptyList())

    fun scrollToTop() {
        scope.launch {
            state.scrollToTop()
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        api.myInventory {
            inventory = it
        }
        isLoading = false
    }

    if (showInventoryItem != null) {
        InventoryItemDialog(
            {
                showInventoryItem = null
            },
            showInventoryItem!!,
            onDrop = {
                // todo
            }
        ) {
            showTradeDialog = showInventoryItem
            showInventoryItem = null
        }
    }

    if (showTradeDialog != null) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            {
                showTradeDialog = null
            },
            title = stringResource(R.string.trade),
            confirmFormatter = defaultConfirmFormatter(
                R.string.trade,
                R.string.trade,  // todo trade with X
                R.string.trade,  // todo trade with X and X
                R.string.trade   // todo trade with X people
            ) { it.name ?: someone },
            omit = { it.id == me?.id },
            multiple = true,
            onPeopleSelected = {
                // todo start trade
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
