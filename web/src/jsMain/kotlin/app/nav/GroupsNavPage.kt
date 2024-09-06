package app.nav

import IndicatorSource
import androidx.compose.runtime.*
import api
import app.ailaai.api.createGroup
import app.ailaai.api.group
import app.ailaai.api.groups
import app.ailaai.api.updateGroup
import app.components.Spacer
import app.dialog.inputDialog
import app.group.GroupItem
import app.menu.Menu
import appString
import appText
import application
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.Member
import com.queatz.db.people
import components.IconButton
import components.Loading
import components.ProfilePhoto
import indicator
import joins
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.toJSDate
import lib.differenceInMinutes
import lib.formatDistanceToNow
import lib.formatDistanceToNowStrict
import notBlank
import opensavvy.compose.lazy.LazyColumn
import opensavvy.compose.lazy.LazyRow
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import push
import r
import sortedDistinct
import kotlin.js.Date

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

    var filterMenuTarget by remember {
        mutableStateOf<DOMRect?>(null)
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

    val categories by remember(groups) {
        mutableStateOf(groups.mapNotNull { it.group?.categories }.flatten().sortedDistinct())
    }

    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(selected) {
        searchText = ""
        showSearch = false
    }

    LaunchedEffect(groups, selected) {
        indicator.set(IndicatorSource.Group, groups.count {
            val myMember = it.members?.firstOrNull { it.person?.id == me?.id }

            it.isUnread(myMember?.member) && it.group?.id != (selected as? GroupNav.Selected)?.group?.group?.id
        })
    }

    val shownGroups = remember(groups, searchText, selectedCategory) {
        val search = searchText.trim()
        if (searchText.isBlank()) {
            groups
        } else {
            groups.filter {
                (it.group?.name?.contains(search, true)
                    ?: false) || (it.members?.any { it.person?.name?.contains(search, true) ?: false } ?: false)
            }
        }.let {
            if (selectedCategory == null) {
                it
            } else {
                it.filter { it.group?.categories?.contains(selectedCategory) == true }
            }
        }
    }

    suspend fun reload(emit: Boolean = true) {
        // todo is next line needed? it was added to wait for the token since groups are the first thing to appear after signing in
        application.bearerToken.first { it != null }
        api.groups {
            groups = it
            if (emit) {
                groups.firstOrNull { it.group?.id == (selected as? GroupNav.Selected)?.group?.group?.id }?.let {
                    GroupNav.Selected(it)
                }?.let { groupNav ->
                    onSelected(groupNav)
                }
            }
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

    if (filterMenuTarget != null) {
        Menu(
            onDismissRequest = { filterMenuTarget = null },
            target = filterMenuTarget!!
        ) {
            categories.forEach { category ->
                item(category, icon = if (category == selectedCategory) "check" else null) {
                    if (category == selectedCategory) {
                        selectedCategory = null
                    } else {
                        selectedCategory = category
                    }
                }
            }
        }
    }

    NavTopBar(me, appString { this.groups }, onProfileClick = onProfileClick) {
        IconButton("filter_list", appString { filter }, count = selectedCategory?.let { 1 } ?: 0, styles = {
        }) {
            filterMenuTarget = if (filterMenuTarget == null) {
                (it.target as HTMLElement).getBoundingClientRect()
            } else {
                null
            }
        }
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
                val people = remember(shownGroups) {
                    shownGroups.people().filter { it.id != me?.id }
                }

                LazyColumn {
                    if (!showSearch) {
                        item {
                            LazyRow({
                                classes(Styles.personList)

                                style {
                                    marginBottom(1.r)
                                }
                            }) {
                                items(people, key = { it.id!! }) { person ->
                                    Div({
                                        classes(Styles.personItem)

                                        title(person.name.orEmpty())

                                        onClick {
                                            window.open("/profile/${person.id}", "_blank")
                                        }
                                    }) {
                                        ProfilePhoto(person, size = 54.px)
                                        Div({
                                            style {
                                                opacity(.5f)
                                                whiteSpace("nowrap")
                                            }
                                        }) {
                                            person.seen?.let {
                                                Text(
                                                    if (differenceInMinutes(Date(), it.toJSDate(), js("{ roundingMethod: \"floor\" }")) < 1) {
                                                        appString { now }
                                                    } else {
                                                        formatDistanceToNowStrict(
                                                            Date(it.toEpochMilliseconds()),
                                                            js("{ includeSeconds: false }")
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    items(shownGroups, key = { it.group!!.id!! }) { group ->
                        GroupItem(
                            group,
                            selected = (selected as? GroupNav.Selected)?.group?.group?.id == group.group?.id,
                            showInCall = true,
                            onSelected = {
                                onSelected(GroupNav.Selected(group))
                                scope.launch {
                                    reload(emit = false)
                                }
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
