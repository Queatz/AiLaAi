package app.nav

import IndicatorSource
import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.createGroup
import app.ailaai.api.friendStatuses
import app.ailaai.api.group
import app.ailaai.api.groups
import app.ailaai.api.myStatus
import app.ailaai.api.recentStatuses
import app.ailaai.api.updateGroup
import app.components.Spacer
import app.dialog.editStatusDialog
import app.dialog.inputDialog
import app.dialog.personStatusDialog
import app.group.GroupItem
import app.menu.Menu
import appString
import appText
import application
import baseUrl
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.Member
import com.queatz.db.Person
import com.queatz.db.PersonStatus
import com.queatz.db.Status
import com.queatz.db.people
import components.IconButton
import components.LazyColumn
import components.LazyRow
import components.Loading
import components.ProfilePhoto
import ellipsize
import indicator
import joins
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import time.differenceInMinutes
import time.formatDistanceToNowStrict
import notBlank
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position.Companion.Absolute
import org.jetbrains.compose.web.css.Position.Companion.Relative
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.div
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import push
import r
import sortedDistinct
import kotlin.js.Date
import kotlin.time.Duration.Companion.minutes

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
    onProfileClick: () -> Unit,
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

    var searchText by remember(showSearch) {
        mutableStateOf("")
    }

    val categories by remember(groups) {
        mutableStateOf(groups.mapNotNull { it.group?.categories }.flatten().sortedDistinct())
    }

    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }

    var statuses by remember { mutableStateOf(emptyMap<String, PersonStatus?>()) }

    var recentStatuses by remember { mutableStateOf(emptyList<Status>()) }

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
        }.sortedByDescending {
            it.pin == true
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

    LaunchedEffect(groups) {
        api.friendStatuses {
            statuses = it.groupBy { it.person!! }.mapValues { it.value.firstOrNull() }
        }
    }

    LaunchedEffect(groups) {
        api.recentStatuses {
            recentStatuses = it
        }
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

    // todo remove selectedGroup
    LaunchedEffect(me, (selected as? GroupNav.Selected)?.group?.group?.id) {
        reload()
        isLoading = false
        groupUpdates.collectLatest {
            reload()
        }
    }

    fun onFriendClick(person: Person, sendMessage: Boolean = false) {
        val status = statuses[person.id!!]

        if (person.id == me?.id) {
            scope.launch {
                editStatusDialog(
                    statuses = recentStatuses,
                    initialStatus = status
                ) {
                    scope.launch {
                        api.myStatus(it) {
                            reload(emit = false)
                        }
                    }
                }
            }
        } else if (status == null || sendMessage) {
            // todo: send message instead
            window.open("/profile/${person.id}", "_blank")
        } else  {
            scope.launch {
                val result = personStatusDialog(person, status, scope)

                if (result == true) {
                    onFriendClick(person, sendMessage = true)
                }
            }
        }
    }

    if (filterMenuTarget != null) {
        Menu(
            onDismissRequest = { filterMenuTarget = null },
            target = filterMenuTarget!!
        ) {
            categories.forEach { category ->
                item(category, icon = if (category == selectedCategory) "check" else null) {
                    selectedCategory = if (category == selectedCategory) {
                        null
                    } else {
                        category
                    }
                }
            }
        }
    }

    NavTopBar(me, appString { this.groups }, onProfileClick = onProfileClick) {
        IconButton("search", appString { search }, styles = {
        }) {
            showSearch = !showSearch
        }
        IconButton("filter_list", appString { filter }, count = selectedCategory?.let { 1 } ?: 0, styles = {
        }) {
            filterMenuTarget = if (filterMenuTarget == null) {
                (it.target as HTMLElement).getBoundingClientRect()
            } else {
                null
            }
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
                padding(1.r / 2)
            }
        }) {
            if (!showSearch && selectedCategory == null) {
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
                val people = remember(shownGroups) { shownGroups.people() }

                LazyColumn {
                    if (!showSearch && selectedCategory == null) {
                        item {
                            LazyRow({
                                classes(Styles.personList)

                                style {
                                    marginBottom(1.r)
                                }
                            }) {
                                items(people) { person ->
                                    Div({
                                        classes(Styles.personItem)

                                        title(person.name.orEmpty())

                                        onClick {
                                            onFriendClick(person)
                                        }
                                    }) {
                                        Div({
                                            style {
                                                position(Relative)
                                            }
                                        }) {
                                            ProfilePhoto(person = person, size = 54.px)
                                            statuses[person.id!!]?.let { status ->
                                                // Status note
                                                if (status.note?.notBlank != null || status.photo?.notBlank != null) {
                                                    Div({
                                                        classes(Styles.personItemStatus)
                                                    }) {
                                                        status.photo?.let { photo ->
                                                            Div({
                                                                style {
                                                                    backgroundImage("url($baseUrl${photo})")
                                                                    backgroundPosition("center")
                                                                    backgroundSize("cover")
                                                                    borderRadius(0.25.r)
                                                                    height(.75.r)
                                                                    width(.75.r)
                                                                    flexShrink(0)
                                                                }
                                                            }) {}
                                                        }
                                                        status.note?.let { note ->
                                                            Span({
                                                                style {
                                                                    ellipsize()
                                                                }
                                                            }) {
                                                                Text(note)
                                                            }
                                                        }
                                                    }
                                                }

                                                // Status indicator
                                                status.statusInfo?.let { status ->
                                                    Div({
                                                        classes(Styles.personItemStatusIndicator)

                                                        style {
                                                            position(Absolute)
                                                            bottom(.125.r)
                                                            right(.125.r)
                                                            backgroundColor(Color(status.color ?: "#ffffff"))
                                                        }
                                                    }) {
                                                        if (person.seen?.let { it < Clock.System.now() - 30.minutes } == true) {
                                                            Span({
                                                                classes(Styles.personItemStatusIndicatorText)
                                                            }) {
                                                                appText { z }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Div({
                                            style {
                                                opacity(.5f)
                                                whiteSpace("nowrap")
                                            }
                                        }) {
                                            person.seen?.let {
                                                Text(
                                                    // todo: translate
                                                    if (person.id == me?.id) {
                                                        appString { setStatus }
                                                    } else if (
                                                        differenceInMinutes(
                                                            date = Date(),
                                                            otherDate = it.toJSDate(),
                                                        ) < 1
                                                    ) {
                                                        appString { now }
                                                    } else {
                                                        formatDistanceToNowStrict(
                                                            date = Date(it.toEpochMilliseconds()),
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

                    items(shownGroups) { group ->
                        GroupItem(
                            group = group,
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
