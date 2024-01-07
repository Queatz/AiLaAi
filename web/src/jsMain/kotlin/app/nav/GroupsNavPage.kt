package app.nav

import androidx.compose.runtime.*
import api
import app.ailaai.api.createGroup
import app.ailaai.api.group
import app.ailaai.api.groups
import app.ailaai.api.updateGroup
import app.components.Spacer
import app.dialog.inputDialog
import app.group.GroupItem
import appString
import appText
import application
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.Member
import components.IconButton
import components.Loading
import indicator
import joins
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import notBlank
import opensavvy.compose.lazy.LazyColumn
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import push
import r

sealed class GroupNav {
    data object None : GroupNav()
    data object Friends : GroupNav()
    data object Local : GroupNav()
    data object Saved : GroupNav()
    data class Selected(val group: GroupExtended) : GroupNav()
}

@Composable
fun GroupsNavPage(
    groupUpdates: Flow<Unit>,
    selected: GroupNav,
    onSelected: (GroupNav) -> Unit,
    onProfileClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val me by application.me.collectAsState()
    var isLoading by remember {
        mutableStateOf(true)
    }

    var groups by remember {
        mutableStateOf(emptyList<GroupExtended>())
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    LaunchedEffect(selected) {
        searchText = ""
        showSearch = false
    }

    LaunchedEffect(groups) {
        indicator.set(IndicatorSource.Group, groups.count {
            val myMember = it.members?.firstOrNull { it.person?.id == me?.id }

            it.isUnread(myMember?.member)
        })
    }

    val shownGroups = remember(groups, searchText) {
        val search = searchText.trim()
        if (searchText.isBlank()) {
            groups
        } else {
            groups.filter {
                (it.group?.name?.contains(search, true)
                    ?: false) || (it.members?.any { it.person?.name?.contains(search, true) ?: false } ?: false)
            }
        }
    }

    suspend fun reload() {
        // todo is next line needed? it was added to wait for the token since groups are the first thing to appear after signing in
        application.bearerToken.first { it != null }
        api.groups {
            groups = it
            onSelected(groups.firstOrNull { it.group?.id == (selected as? GroupNav.Selected)?.group?.group?.id }?.let {
                GroupNav.Selected(it)
            } ?: GroupNav.None)
        }
        joins.reload()
    }

    // todo remove selectedGroup
    LaunchedEffect(selected) {
        push.events.collectLatest {
            reload()
        }
    }

    // todo remove selectedGroup
    LaunchedEffect(selected) {
        push.reconnect.collectLatest {
            reload()
        }
    }

    LaunchedEffect(me) {
        reload()
        isLoading = false
    }

    // todo remove selectedGroup
    LaunchedEffect(selected) {
        groupUpdates.collectLatest {
            reload()
        }
    }

    NavTopBar(me, appString { this.groups }, onProfileClick = onProfileClick) {
        IconButton("search", appString { search }, styles = {
        }) {
            showSearch = !showSearch
        }

        val createGroup = appString { createGroup }
        val groupName = appString { groupName }
        val create = appString { create }

        IconButton("add", appString { this.createGroup }, styles = {
            marginRight(.5.r)
        }) {
            scope.launch {
                val result = inputDialog(
                    createGroup,
                    groupName,
                    create
                )

                if (result == null) return@launch

                api.createGroup(emptyList()) { group ->
                    api.updateGroup(group.id!!, Group(name = result))
                    reload()
                    api.group(group.id!!) {
                        onSelected(GroupNav.Selected(it))
                    }
                }
            }
        }
    }

    if (showSearch) {
        NavSearchInput(searchText, { searchText = it }, onDismissRequest = {
            searchText = ""
            showSearch = false
        })
    }

    if (isLoading) {
        Loading()
    } else {
        Div({
            style {
                overflowY("auto")
                overflowX("hidden")
                property("scrollbar-width", "none")
                padding(1.r / 2)
            }
        }) {
            if (!showSearch) {
//                NavMenuItem("group", "Friends", selected = selected is GroupNav.Friends) {
//                    onSelected(GroupNav.Friends)
//                }
                NavMenuItem("location_on", appString { local }, selected = selected is GroupNav.Local) {
                    onSelected(GroupNav.Local)
                }
                Spacer()
            }

            if (shownGroups.isEmpty()) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        justifyContent(JustifyContent.Center)
                        opacity(.5)
                        padding(1.r)
                    }
                }) {
                    appText { noGroups }
                }
            } else {
                LazyColumn {
                    items(shownGroups, key = { it.group!!.id!! }) { group ->
                        GroupItem(
                            group,
                            selected = (selected as? GroupNav.Selected)?.group?.group?.id == group.group?.id,
                            onSelected = {
                                onSelected(GroupNav.Selected(group))
                            },
                        )
                    }
                }
            }
        }
    }
}

fun GroupExtended.name(someone: String, emptyGroup: String, omit: List<String>) =
    group?.name?.notBlank
        ?: members
            ?.filter { !omit.contains(it.person?.id) }
            ?.mapNotNull { it.person }
            ?.joinToString { it.name ?: someone }
            ?.notBlank
        ?: emptyGroup

fun GroupExtended.isUnread(member: Member?): Boolean {
    return (member?.seen?.toEpochMilliseconds() ?: return false) < (latestMessage?.createdAt?.toEpochMilliseconds()
        ?: return false)
}
