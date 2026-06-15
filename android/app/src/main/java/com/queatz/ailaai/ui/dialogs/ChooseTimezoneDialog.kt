package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import java.util.TimeZone

@Composable
fun ChooseTimezoneDialog(
    onDismissRequest: () -> Unit,
    preselect: String? = null,
    onTimezone: (String?) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val allTimezones = remember { TimeZone.getAvailableIDs().toList() }
    var timezones by remember { mutableStateOf(allTimezones) }
    var selected by remember { mutableStateOf(preselect?.let { listOf(it) } ?: listOf()) }

    LaunchedEffect(searchText) {
        timezones = if (searchText.isBlank()) allTimezones else allTimezones.filter {
            it.contains(searchText, true)
        }
        if (searchText.isNotBlank()) {
            selected = emptyList()
        }
    }

    ChooseDialog(
        onDismissRequest = onDismissRequest,
        isLoading = false,
        title = stringResource(R.string.timezone),
        allowNone = false,
        photoFormatter = null,
        nameFormatter = { it.replace("_", " ") },
        confirmFormatter = {
            if (it.isEmpty()) "" else stringResource(R.string.choose_x, it.first().replace("_", " "))
        },
        textWhenEmpty = { "" },
        searchText = searchText,
        searchTextChange = { searchText = it },
        items = timezones,
        key = { it },
        selected = selected,
        onSelectedChange = {
            selected = it.takeLast(1)
        },
        showSearch = { true },
        onConfirm = {
            onTimezone(it.firstOrNull())
        }
    )
}
