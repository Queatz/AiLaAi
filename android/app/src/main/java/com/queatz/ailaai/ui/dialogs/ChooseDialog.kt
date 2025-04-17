package com.queatz.ailaai.ui.dialogs

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toggle
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.launch

@Composable
fun <T> defaultConfirmFormatter(
    @StringRes none: Int,
    @StringRes one: Int,
    @StringRes two: Int,
    @StringRes many: Int,
    nameFormatter: (T) -> String,
): @Composable (List<T>) -> String = { item ->
    when {
        item.isEmpty() -> stringResource(none)
        item.size == 1 -> stringResource(one, *item.map { nameFormatter(it) }.toTypedArray())
        item.size == 2 -> stringResource(two, *item.map { nameFormatter(it) }.toTypedArray())
        else -> stringResource(many, item.size)
    }
}

@Composable
fun <T> defaultConfirmPluralFormatter(
    count: Int,
    @PluralsRes none: Int,
    @PluralsRes one: Int,
    @PluralsRes two: Int,
    @PluralsRes many: Int,
    nameFormatter: (T) -> String,
): @Composable (List<T>) -> String = { item ->
    when {
        item.isEmpty() -> pluralStringResource(none, count, count)
        item.size == 1 -> pluralStringResource(one, count, count, *item.map { nameFormatter(it) }.toTypedArray())
        item.size == 2 -> pluralStringResource(two, count, count, *item.map { nameFormatter(it) }.toTypedArray())
        else -> pluralStringResource(many, count, count, item.size)
    }
}

@Composable
fun <T> ChooseDialog(
    onDismissRequest: () -> Unit,
    isLoading: Boolean,
    title: String,
    allowNone: Boolean,
    photoFormatter: @Composable ((T) -> List<ContactPhoto>)?,
    nameFormatter: @Composable (T) -> String,
    confirmFormatter: @Composable (List<T>) -> String,
    textWhenEmpty: @Composable (isSearchBlank: Boolean) -> String,
    extraButtons: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    infoFormatter: (@Composable (T) -> String?)? = null,
    maxSelectedCount: Int = 50,
    multiple: Boolean = true,
    searchText: String,
    searchTextChange: (String) -> Unit,
    items: List<T>,
    key: (item: T) -> String,
    selected: List<T>,
    onSelectedChange: (List<T>) -> Unit,
    showSearch: (List<T>) -> Boolean = { it.size > 5 },
    state: LazyListState = rememberLazyListState(),
    onQrCodeScan: ((ScanQrCodeResult) -> Unit)? = null,
    onConfirm: suspend (List<T>) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current!!
    val coroutineScope = rememberCoroutineScope()
    var disableSubmit by rememberStateOf(true)

    LaunchedEffect(selected) {
        disableSubmit = (selected.isEmpty() && !allowNone) || selected.size > maxSelectedCount
    }

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(bottom = 1.pad)
                    .fillMaxWidth()
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                )

                if (onQrCodeScan != null) {
                    ScanQrCodeButton(onQrCodeScan)
                }

                actions()
            }
            if (searchText.isNotEmpty() || showSearch(items)) {
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
                        .padding(bottom = 1.pad)
                )
            }
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier
                    .weight(1f, fill = items.size > 5)
            ) {
                if (isLoading) {
                    item {
                        Loading()
                    }
                } else if (items.isEmpty()) {
                    item {
                        Text(
                            textWhenEmpty(searchText.isBlank()),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(2.pad)
                        )
                    }
                } else {
                    items(items, key = key) {
                        val isSelected = selected.any { item -> key(item) == key(it) }
                        GroupMember(
                            photoFormatter?.invoke(it),
                            nameFormatter(it),
                            infoFormatter?.invoke(it),
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
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selected.isEmpty()) {
                    extraButtons()
                }
                TextButton(
                    onClick = {
                        if (multiple) {
                            disableSubmit = true

                            coroutineScope.launch {
                                try {
                                    onConfirm(selected)
                                    onDismissRequest()
                                } finally {
                                    disableSubmit = false
                                }
                            }
                        } else {
                            onDismissRequest()
                        }
                    },
                    enabled = !disableSubmit && (allowNone || !isLoading),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = confirmFormatter(selected),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.5f, false)
                    )
                    if (multiple) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 1.pad)
                        )
                    }
                }
            }
        }
    }
}
