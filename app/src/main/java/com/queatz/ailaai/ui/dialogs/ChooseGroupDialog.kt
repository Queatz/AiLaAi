package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.photos


@Composable
fun ChooseGroupDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmFormatter: @Composable (List<GroupExtended>) -> String,
    me: Person?,
    omit: List<Group> = emptyList(),
    onGroupsSelected: suspend (List<Group>) -> Unit
) {

    var isLoading by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var allGroups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var groups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var selected by remember { mutableStateOf(listOf<GroupExtended>()) }

    LaunchedEffect(true) {
        isLoading = true
        try {
            allGroups = api.groups()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isLoading = false
    }

    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)

    LaunchedEffect(allGroups, selected, searchText) {
        val all = allGroups
            .filter { omit.none { group -> it.group?.id == group.id } }
        groups = (if (searchText.isBlank()) all else all.filter {
            it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()).contains(searchText, true)
        })
    }

    ChooseDialog(
        onDismissRequest = onDismissRequest,
        isLoading = isLoading,
        title = title,
        allowNone = false,
        photoFormatter = { it.photos(me?.let(::listOf) ?: emptyList()) },
        nameFormatter = { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) },
        confirmFormatter = confirmFormatter,
        textWhenEmpty = { isBlank -> stringResource(if (isBlank) R.string.you_have_no_conversations else R.string.no_conversations_to_show) },
        searchText = searchText,
        searchTextChange = { searchText = it },
        items = groups,
        key = { it.group!!.id!! },
        selected = selected,
        onSelectedChange = { selected = it },
        onConfirm = { onGroupsSelected(it.mapNotNull { it.group }) }
    )
}
