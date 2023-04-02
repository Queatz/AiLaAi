package com.queatz.ailaai.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.toggle
import com.queatz.ailaai.ui.components.GroupMember
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@Composable
fun <T> defaultConfirmFormatter(
    @StringRes none: Int,
    @StringRes one: Int,
    @StringRes two: Int,
    @StringRes many: Int,
    nameFormatter: (T) -> String
): @Composable (List<T>) -> String = { item ->
    when {
        item.isEmpty() -> stringResource(none)
        item.size == 1 -> stringResource(one, *item.map { nameFormatter(it) }.toTypedArray())
        item.size == 2 -> stringResource(two, *item.map { nameFormatter(it) }.toTypedArray())
        else -> stringResource(many, item.size)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun <T> ChooseDialog(
    onDismissRequest: () -> Unit,
    isLoading: Boolean,
    title: String,
    allowNone: Boolean,
    photoFormatter: @Composable (T) -> List<ContactPhoto>,
    nameFormatter: @Composable (T) -> String,
    confirmFormatter: @Composable (List<T>) -> String,
    textWhenEmpty: @Composable (isSearchBlank: Boolean) -> String,
    searchText: String,
    searchTextChange: (String) -> Unit,
    items: List<T>,
    key: (item: T) -> String,
    selected: List<T>,
    onSelectedChange: (List<T>) -> Unit,
    onConfirm: suspend (List<T>) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current!!
    val coroutineScope = rememberCoroutineScope()
    var disableSubmit by remember { mutableStateOf(true) }

    LaunchedEffect(selected) {
        disableSubmit = (selected.isEmpty() && !allowNone) || selected.size >= 50
    }

    Dialog(onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .padding(PaddingDefault * 2)
                .fillMaxHeight(.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(PaddingDefault * 3)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = PaddingDefault)
                )
                OutlinedTextField(
                    searchText,
                    onValueChange = searchTextChange,
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
                        .padding(bottom = PaddingDefault)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    if (isLoading) {
                        item {
                            LinearProgressIndicator(
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PaddingDefault)
                            )
                        }
                    } else if (items.isEmpty()) {
                        item {
                            Text(
                                textWhenEmpty(searchText.isBlank()),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(PaddingDefault * 2)
                            )
                        }
                    } else {
                        items(items, key = key) {
                            val isSelected = selected.any { item -> key(item) == key(it) }
                            GroupMember(
                                photoFormatter(it),
                                nameFormatter(it),
                                null,
                                isSelected
                            ) {
                                onSelectedChange(
                                    selected.toggle(it) { item -> key(item) == key(it) }
                                )
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        {
                            disableSubmit = true

                            coroutineScope.launch {
                                try {
                                    onConfirm(selected)
                                    onDismissRequest()
                                } finally {
                                    disableSubmit = false
                                }
                            }
                        },
                        enabled = !disableSubmit,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(confirmFormatter(selected), textAlign = TextAlign.End, modifier = Modifier.weight(0.5f, false))
                        Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.padding(start = PaddingDefault))
                    }
                }
            }
        }
    }
}
