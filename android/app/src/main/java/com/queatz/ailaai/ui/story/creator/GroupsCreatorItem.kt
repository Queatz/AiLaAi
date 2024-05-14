package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.ailaai.api.createGroup
import app.ailaai.api.group
import app.ailaai.api.updateGroup
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.GroupInfo
import com.queatz.ailaai.ui.components.LoadingText
import com.queatz.ailaai.ui.components.SearchResult
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.ailaai.ui.story.ReorderDialog
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.StoryContent

fun LazyGridScope.groupsCreatorItem(creatorScope: CreatorScope<StoryContent.Groups>) = with(creatorScope) {
    itemsIndexed(
        part.groups,
        span = { _, _ -> GridItemSpan(maxLineSpan) },
        key = { index, it -> "${part.hashCode()}.$it" }
    ) { index, groupId ->
        val context = LocalContext.current
        val me = me
        val nav = nav
        var group by remember { mutableStateOf<GroupExtended?>(null) }
        var showGroupMenu by rememberStateOf(false)
        var showAddGroupDialog by rememberStateOf(false)
        var showCreateGroupDialog by rememberStateOf(false)
        var showReorderDialog by rememberStateOf(false)

        if (showCreateGroupDialog) {
            TextFieldDialog(
                onDismissRequest = { showCreateGroupDialog = false },
                title = stringResource(R.string.group_name),
                button = stringResource(R.string.create_group),
                singleLine = true,
                placeholder = stringResource(R.string.empty_group_name),
                requireModification = false
            ) { value ->
                api.createGroup(emptyList()) { group ->
                    if (value.isNotBlank()) {
                        api.updateGroup(group.id!!, Group(name = value))
                    }
                    edit {
                        groups += group.id!!
                    }
                }
                showCreateGroupDialog = false
            }
        }

        if (showAddGroupDialog) {
            val someone = stringResource(R.string.someone)
            val emptyGroup = stringResource(R.string.empty_group_name)

            ChooseGroupDialog(
                {
                    showAddGroupDialog = false
                },
                title = stringResource(R.string.add_group),
                actions = {
                    IconButton(
                        {
                            showAddGroupDialog = false
                            showCreateGroupDialog = true
                        }
                    ) {
                        Icon(Icons.Outlined.Add, null)
                    }
                },
                confirmFormatter = defaultConfirmFormatter(
                    R.string.choose_group,
                    R.string.choose_x,
                    R.string.choose_x_and_x,
                    R.string.choose_x_groups
                ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) },
                infoFormatter = {
                    buildString {
                        val count = it.members?.size ?: 0
                        append("$count ")
                        append(context.resources.getQuantityString(R.plurals.inline_members, count))
                        if (it.group?.description.isNullOrBlank().not()) {
                            append(" • ")
                            append(it.group!!.description)
                        }
                    }
                },
                filter = {
                    it.group?.open == true && part.groups.none { id -> it.group?.id == id }
                }
            ) {
                edit {
                    groups += it
                        .mapNotNull { it.id }
                        .distinctBy { it }
                }
            }
        }

        if (showReorderDialog) {
            ReorderDialog(
                { showReorderDialog = false },
                onMove = { from, to ->
                    edit {
                        groups = groups.toMutableList().apply {
                            add(to.index, removeAt(from.index))
                        }
                    }
                },
                list = true,
                items = part.groups,
                key = { it }
            ) { groupId, elevation ->
                var group by remember { mutableStateOf<GroupExtended?>(null) }

                LaunchedEffect(groupId) {
                    api.group(groupId) { group = it }
                }

                LoadingText(group != null, stringResource(R.string.loading_group)) {
                    ContactItem(
                        onClick = null,
                        onLongClick = null,
                        item = SearchResult.Group(group!!),
                        info = GroupInfo.LatestMessage
                    )
                }
            }
        }

        if (showGroupMenu) {
            Menu(
                {
                    showGroupMenu = false
                }
            ) {
                menuItem(stringResource(if (part.coverPhotos) R.string.hide_photos else R.string.show_photos)) {
                    showGroupMenu = false
                    edit {
                        coverPhotos = !coverPhotos
                    }
                }
                menuItem(stringResource(R.string.add_group)) {
                    showGroupMenu = false
                    showAddGroupDialog = true
                }
                menuItem(stringResource(R.string.create_group)) {
                    showGroupMenu = false
                    showCreateGroupDialog = true
                }
                menuItem(stringResource(R.string.open_group_action)) {
                    showGroupMenu = false
                    nav.navigate(AppNav.Group(groupId))
                }
                if (part.groups.size > 1) {
                    menuItem(stringResource(R.string.reorder)) {
                        showGroupMenu = false
                        showReorderDialog = true
                    }
                }
                menuItem(stringResource(R.string.remove)) {
                    showGroupMenu = false
                    if (part.groups.size == 1) {
                        showGroupMenu = false
                        remove(partIndex)
                    } else {
                        edit {
                            groups = groups.toMutableList().apply {
                                removeAt(index)
                            }
                        }
                    }
                }
                if (part.groups.size > 1) {
                    menuItem(stringResource(R.string.remove_all)) {
                        showGroupMenu = false
                        remove(partIndex)
                    }
                }
            }
        }

        LaunchedEffect(groupId) {
            api.group(groupId) { group = it }
        }

        LoadingText(group != null, stringResource(R.string.loading_group)) {
            ContactItem(
                onClick = {
                    showGroupMenu = true
                },
                onLongClick = {},
                SearchResult.Group(group!!),
                info = GroupInfo.LatestMessage,
                coverPhoto = part.coverPhotos
            )
        }
    }
}
