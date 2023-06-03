package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.api.groups
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.photos
import com.queatz.ailaai.extensions.rememberStateOf


@Composable
fun ChooseGroupDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmFormatter: @Composable (List<GroupExtended>) -> String,
    me: Person?,
    omit: List<Group> = emptyList(),
    filter: (GroupExtended) -> Boolean = { true },
    extraButtons: @Composable RowScope.() -> Unit = {},
    allowNone: Boolean = false,
    preselect: List<Group>? = null,
    onGroupsSelected: suspend (List<Group>) -> Unit
) {
    var isLoading by rememberStateOf(false)
    var hasPreselected by rememberStateOf(false)
    var searchText by remember { mutableStateOf("") }
    var allGroups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var groups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var selected by remember { mutableStateOf(listOf<GroupExtended>()) }

    LaunchedEffect(Unit) {
        isLoading = true
        api.groups {
            allGroups = it
        }
        isLoading = false
    }

    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)

    LaunchedEffect(allGroups, selected, searchText) {
        val all = allGroups
            .filter { omit.none { group -> it.group?.id == group.id } }
            .filter(filter)
        groups = (if (searchText.isBlank()) all else all.filter {
            it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()).contains(searchText, true)
        })
        if (!hasPreselected && groups.isNotEmpty()) {
            if (!preselect.isNullOrEmpty() && selected.isEmpty()) {
                selected = groups.filter { group -> preselect.any { it.id == group.group?.id } }
            }
            hasPreselected = true
        }
    }

    ChooseDialog(
        onDismissRequest = onDismissRequest,
        isLoading = isLoading,
        title = title,
        allowNone = allowNone,
        extraButtons = extraButtons,
        photoFormatter = { it.photos(me?.let(::listOf) ?: emptyList(), ifEmpty = me?.let(::listOf)) },
        nameFormatter = { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) },
        confirmFormatter = confirmFormatter,
        textWhenEmpty = { isBlank -> stringResource(if (isBlank) R.string.you_have_no_groups else R.string.no_groups_to_show) },
        searchText = searchText,
        searchTextChange = { searchText = it },
        items = groups,
        key = { it.group!!.id!! },
        selected = selected,
        onSelectedChange = { selected = it },
        onConfirm = { onGroupsSelected(it.mapNotNull { it.group }) }
    )
}
