package com.queatz.ailaai.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Item
import com.queatz.db.ItemExtended
import com.queatz.db.MintItemBody
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mintItem
import myItems
import kotlin.time.Duration.Companion.seconds

@Composable
fun MyItemsScreen() {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var search by rememberSavableStateOf("")
    var isLoading by rememberStateOf(false)
    var showMintItem by rememberStateOf<Item?>(null)
    var showCreateItemDialog by rememberStateOf(false)
    var items by rememberStateOf<List<ItemExtended>>(emptyList())
    val context = LocalContext.current

    fun scrollToTop() {
        scope.launch {
            state.scrollToTop()
        }
    }

    suspend fun reload() {
        isLoading = true
        api.myItems {
            items = it
        }
        isLoading = false
    }

    val addedToInventory = stringResource(R.string.added_to_inventory)

    fun mint(item: Item, quantity: Double) {
        scope.launch {
            api.mintItem(item.id!!, MintItemBody(quantity = quantity)) {
                context.toast(addedToInventory)
            }
            reload()
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    if (showCreateItemDialog) {
        CreateItemDialog({
            showCreateItemDialog = false
        }) {
            reload()
            showCreateItemDialog = false
        }
    }

    if (showMintItem != null) {
        TextFieldDialog(
            onDismissRequest = {
                showMintItem = null
            },
            title = showMintItem?.name,
            button = stringResource(R.string.mint),
            placeholder = stringResource(R.string.quantity),
            requireNotBlank = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (showMintItem?.divisible == true) {
                    KeyboardType.Decimal
                } else {
                    KeyboardType.Number
                }
            ),
            valueFormatter = {
                if (showMintItem?.divisible == true) {
                    if (it.isNumericTextInput()) it else null
                } else {
                    if (it.isNumericTextInput(allowDecimal = false)) it else null
                }
            },
            extraContent = {
                Text(
                    showMintItem?.description ?: "",
                    modifier = Modifier
                        .padding(bottom = 1.pad)
                )
            },
            bottomContent = {
                if (showMintItem?.lifespan != null) {
                    var expiration = Clock.System.now() + (showMintItem!!.lifespan!!.seconds)

                    LaunchedEffect(Unit) {
                        delayUntilNextMinute()
                        expiration = Clock.System.now() + (showMintItem!!.lifespan!!.seconds)
                    }

                    Text(
                        stringResource(
                            R.string.expires_x,
                            "${expiration.timeUntil()} ${stringResource(R.string.inline_on)} ${expiration.formatDateAndTime()}"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 1.pad)
                    )
                } else {
                    Text(
                        stringResource(R.string.never_expires),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 1.pad)
                    )
                }
            }
        ) {
            it.toDoubleOrNull()?.let {
                mint(showMintItem!!, it)
                showMintItem = null
            } ?: context.showDidntWork()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppHeader(
                stringResource(R.string.items),
                {
                    scrollToTop()
                },
            ) {
                // todo actions
            }
            if (isLoading) {
                Loading()
            } else {
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 1.pad,
                        end = 1.pad,
                        top = 1.pad,
                        bottom = 1.pad + 80.dp
                    )
                ) {
                    items(items, key = { it.item!!.id!! }) {
                        ItemLayout(it) {
                            showMintItem = it.item
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
                    Icon(Icons.Outlined.Add, "")
                }
            ) {
                showCreateItemDialog = true
            }
        }
    }
}
