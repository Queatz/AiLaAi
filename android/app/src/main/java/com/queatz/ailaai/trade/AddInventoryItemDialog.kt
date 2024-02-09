package com.queatz.ailaai.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.item.InventoryItemLayout
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.InventoryItemExtended
import myInventory

@Composable
fun AddInventoryItemDialog(
    onDismissRequest: () -> Unit,
    omit: List<String> = emptyList(),
    onInventoryItem: (InventoryItemExtended) -> Unit
) {
    var searchText by rememberStateOf("")
    var isLoading by rememberStateOf(false)
    var items by rememberStateOf(emptyList<InventoryItemExtended>())
    val keyboardController = LocalSoftwareKeyboardController.current!!

    var shownItems = remember(searchText, items) {
        if (searchText.isBlank()) {
            items
        } else {
            items.filter {
                it.item!!.name!!.contains(searchText, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        api.myInventory {
            items = it.filter {
                it.inventoryItem!!.id !in omit
            }
        }
        isLoading = false
    }

    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            scrollable = false,
            content = {
                if (searchText.isNotEmpty() || items.size > 5) {
                    OutlinedTextField(
                        searchText,
                        onValueChange = { searchText = it },
                        label = { Text(stringResource(R.string.search)) },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController.hide()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.pad)
                    )
                }
                LazyVerticalGrid(
                    state = rememberLazyGridState(),
                    columns = GridCells.Adaptive(96.dp),
                    verticalArrangement = Arrangement.spacedBy(1.pad),
                    horizontalArrangement = Arrangement.spacedBy(1.pad),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(shownItems) {
                        InventoryItemLayout(it) {
                            onInventoryItem(it)
                        }
                    }

                    if (shownItems.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyText(
                                stringResource(R.string.no_items)
                            )
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
            }
        )
    }
}
