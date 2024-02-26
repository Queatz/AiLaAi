package com.queatz.ailaai.ui.screens

import android.Manifest
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import app.ailaai.api.*
import at.bluesource.choicesdk.maps.common.LatLng
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.joins
import com.queatz.ailaai.services.messages
import com.queatz.ailaai.services.push
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.Member
import com.queatz.db.Person
import com.queatz.push.GroupPushData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.datetime.Instant

private var cache = mutableMapOf<MainTab, List<GroupExtended>>()

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FriendsScreen() {
    val state = rememberLazyListState()
    var searchText by rememberSaveable { mutableStateOf("") }
    var allHiddenGroups by remember { mutableStateOf(emptyList<GroupExtended>()) }
    var allPeople by remember { mutableStateOf(emptyList<Person>()) }
    var results by remember { mutableStateOf(emptyList<SearchResult>()) }
    var createGroupName by remember { mutableStateOf("") }
    var showCreateGroupName by rememberStateOf(false)
    var showCreateGroupMembers by rememberStateOf(false)
    var showPushPermissionDialog by rememberStateOf(false)
    var showHiddenGroupsDialog by rememberStateOf(false)
    var showSharedGroupsDialogPerson by rememberStateOf<Person?>(null)
    var showSharedGroupsDialog by rememberStateOf<List<GroupExtended>>(emptyList())
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedHiddenGroups by rememberStateOf(listOf<Group>())
    var tab by rememberSavableStateOf(MainTab.Friends)
    var allGroups by remember { mutableStateOf(
        cache[tab] ?: emptyList()
    ) }
    var isLoading by rememberStateOf(allGroups.isEmpty())
    var geo: LatLng? by remember { mutableStateOf(null) }
    val nav = nav
    val me = me
    val locationSelector = locationSelector(
        geo,
        { geo = it },
        nav.context as Activity
    )
    val reloadFlow = remember {
        MutableSharedFlow<Boolean>()
    }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var categories by rememberSaveable { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(geo) {
        geo?.let {
            api.myGeo(it.toGeo())
        }
    }

    LaunchedEffect(allGroups) {
        cache[tab] = allGroups
        categories = allGroups
            .flatMap { it.group?.categories ?: emptyList() }
            .sortedDistinct()
    }

    fun update() {
        results = allPeople
            .map { SearchResult.Connect(it) } +
                searchText.trim().let { text ->
                    (if (text.isBlank()) allGroups else allGroups.filter {
                        (it.group?.name?.contains(text, true) ?: false) ||
                                it.members?.any {
                                    it.person?.id != me?.id && it.person?.name?.contains(
                                        text,
                                        true
                                    ) ?: false
                                } ?: false
                    }).map { SearchResult.Group(it) }
                }.let {
                    if (selectedCategory == null) it else it.filter {
                        it.groupExtended.group?.categories?.contains(selectedCategory) == true
                    }
                }
    }

    suspend fun reload(passive: Boolean = false) {
        isLoading = results.isEmpty()
        when (tab) {
            MainTab.Friends -> {
                api.groups(
                    onError = {
                        if (!passive && it !is CancellationException) {
                            context.showDidntWork()
                        }
                    }
                ) {
                    allGroups = it.filter { it.group != null }
                    messages.refresh(me, allGroups)
                }
            }

            MainTab.Local -> {
                if (geo != null) {
                    api.exploreGroups(
                        geo!!.toGeo(),
                        searchText,
                        public = true,
                        onError = {
                            if (!passive && it !is CancellationException) {
                                context.showDidntWork()
                            }
                        }
                    ) {
                        allGroups = it.filter { it.group != null }
                    }
                }
            }

            else -> {}
        }
        update()
        isLoading = false
        scope.launch {
            joins.reload()
        }
    }

    LaunchedEffect(Unit) {
        reloadFlow.collectLatest {
            reload(it)
        }
    }

    LaunchedEffect(Unit) {
        push.events
            .filterIsInstance<GroupPushData>()
            .collectLatest {
                reloadFlow.emit(true)
            }
    }

    ResumeEffect {
        reloadFlow.emit(true)
    }

    LaunchedEffect(Unit) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            if (!notificationPermissionState.status.isGranted) {
                if (notificationPermissionState.status.shouldShowRationale) {
                    notificationPermissionState.launchPermissionRequest()
                } else {
                    showPushPermissionDialog = true
                }
            }
        }
    }

    LaunchedEffect(searchText) {
        if (searchText.isBlank()) {
            allPeople = emptyList()
        } else {
            api.people(searchText) {
                allPeople = it
            }
        }
        update()
        reloadFlow.emit(true)
    }

    LaunchedEffect(selectedCategory) {
        update()
    }

    var skipFirst by rememberStateOf(true)

    LaunchedEffect(geo) {
        if (geo == null || tab != MainTab.Local) {
            return@LaunchedEffect
        }
        if (skipFirst) {
            skipFirst = false
            return@LaunchedEffect
        }
        // todo search server, set allGroups
        reloadFlow.emit(true)
    }

    fun setTab(it: MainTab) {
        tab = it
        allGroups = cache[tab] ?: emptyList()
        allPeople = emptyList()
        update()
        isLoading = allGroups.isEmpty()
        selectedCategory = null
        scope.launch {
            state.scrollToTop()
            reloadFlow.emit(false)
        }
    }

    val tabs = listOf(MainTab.Friends, MainTab.Local)

    if (showSharedGroupsDialog.isNotEmpty()) {
        ChooseGroupDialog(
            {
                showSharedGroupsDialog = emptyList()
            },
            title = showSharedGroupsDialogPerson?.name ?: stringResource(R.string.someone),
            multiple = false,
            allowNone = true,
            confirmFormatter = { stringResource(R.string.close) },
            infoFormatter = { "${it.members?.size ?: 0} ${context.resources.getQuantityString(R.plurals.inline_members, it.members?.size ?: 0)}" },
            groups = {
                showSharedGroupsDialog
            }
        ) { selected ->
            showSharedGroupsDialog = emptyList()
            nav.navigate("group/${selected.first().id!!}")
        }
    }

    if (selectedHiddenGroups.isNotEmpty()) {
        AlertDialog(
            {
                selectedHiddenGroups = emptyList()
            },
            title = {
                Text(pluralStringResource(R.plurals.x_groups, selectedHiddenGroups.size, selectedHiddenGroups.size))
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(1.pad)) {
                    TextButton(
                        {
                            selectedHiddenGroups = emptyList()
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        {
                            scope.launch {
                                coroutineScope {
                                    allHiddenGroups
                                        .filter { group -> selectedHiddenGroups.any { it.id == group.group?.id } }
                                        .mapNotNull { groupExtended ->
                                            val member =
                                                groupExtended.members?.firstOrNull { it.person?.id == me?.id }?.member
                                                    ?: return@mapNotNull null

                                            async {
                                                api.removeMember(member.id!!)
                                            }
                                        }.awaitAll()
                                    reloadFlow.emit(true)
                                    selectedHiddenGroups = emptyList()
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.leave), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    {
                        scope.launch {
                            coroutineScope {
                                allHiddenGroups
                                    .filter { group -> selectedHiddenGroups.any { it.id == group.group?.id } }
                                    .mapNotNull { groupExtended ->
                                        val member =
                                            groupExtended.members?.firstOrNull { it.person?.id == me?.id }?.member
                                                ?: return@mapNotNull null

                                        async {
                                            api.updateMember(
                                                member.id!!,
                                                Member(
                                                    hide = false
                                                )
                                            )
                                        }
                                    }.awaitAll()
                                reloadFlow.emit(true)
                                selectedHiddenGroups = emptyList()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.show))
                }
            })
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppHeader(
            stringResource(R.string.groups),
            {
                scope.launch {
                    state.scrollToTop()
                }
            }
        ) {
            var showMenu by rememberStateOf(false)
            IconButton(
                {
                    showMenu = true
                }
            ) {
                Icon(Icons.Outlined.MoreVert, null)
                Dropdown(showMenu, { showMenu = false }) {
                    DropdownMenuItem({
                        Text(stringResource(R.string.hidden_groups))
                    }, {
                        showMenu = false
                        showHiddenGroupsDialog = true
                    })
                }
            }
            ScanQrCodeButton()
        }
        MainTabs(
            tab,
            {
                setTab(it)
            },
            tabs = tabs
        )
        LocationScaffold(
            geo,
            locationSelector,
            enabled = tab == MainTab.Local
        ) {
            var h by rememberStateOf(80.dp.px)

            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .swipeMainTabs {
                        when (val it = tabs.swipe(tab, it)) {
                            is SwipeResult.Previous -> {
                                nav.navigate("stories")
                            }

                            is SwipeResult.Next -> {
                                nav.navigate("schedule")
                            }

                            is SwipeResult.Select<*> -> {
                                setTab(it.item as MainTab)
                            }
                        }
                    }
            ) {
                LazyColumn(
                    state = state,
                    contentPadding = PaddingValues(
                        1.pad,
                        1.pad,
                        1.pad,
                        3.pad + h.inDp()
                    ),
                    verticalArrangement = Arrangement.spacedBy(1.pad),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLoading) {
                        item {
                            Loading()
                        }
                    } else if (results.isEmpty()) {
                        item {
                            Text(
                                stringResource(
                                    if (searchText.isBlank()) when (tab) {
                                        MainTab.Friends -> R.string.you_have_no_groups
                                        else -> R.string.no_groups_nearby
                                    } else R.string.no_groups_to_show
                                ),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(2.pad)
                            )
                        }
                    } else {
                            item {
                                Column {
                                    AnimatedVisibility(tab == MainTab.Friends && searchText.isBlank() && selectedCategory == null) {
                                        Friends(
                                            remember(allGroups) {
                                                allGroups.people().filter { it.id != me?.id }
                                            },
                                            {
                                                nav.navigate("profile/${it.id!!}")
                                            }
                                        ) { person ->
                                            scope.launch {
                                                api.groupsWith(listOf(me!!.id!!, person.id!!)) {
                                                    if (it.size == 1) {
                                                        nav.navigate("group/${it.first().group!!.id!!}")
                                                    } else {
                                                        showSharedGroupsDialogPerson = person
                                                        showSharedGroupsDialog = it
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        items(
                            results,
                            key = {
                                when (it) {
                                    is SearchResult.Connect -> "connect:${it.person.id}"
                                    is SearchResult.Group -> "group:${it.groupExtended.group!!.id!!}"
                                }
                            }
                        ) {
                            ContactItem(
                                it,
                                {
                                    scope.launch {
                                        reloadFlow.emit(true)
                                    }
                                },
                                when (tab) {
                                    MainTab.Friends -> GroupInfo.LatestMessage
                                    else -> GroupInfo.Members
                                },
                                coverPhoto = tab == MainTab.Local
                            )
                        }
                    }
                }

                PageInput(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onPlaced {
                            h = it.size.height
                        }
                ) {
                    SearchContent(
                        locationSelector,
                        isLoading,
                        categories = categories,
                        category = selectedCategory
                    ) {
                        selectedCategory = it
                    }
                    SearchFieldAndAction(
                        searchText,
                        { searchText = it },
                        placeholder = stringResource(R.string.search_people_and_groups),
                        action = {
                            Icon(Icons.Outlined.Add, stringResource(R.string.new_group))
                        },
                        onAction = {
                            createGroupName = searchText
                            showCreateGroupName = true
                        },
                    )
                }
            }
        }
    }

    if (showHiddenGroupsDialog) {
        var groups by rememberStateOf(listOf<GroupExtended>())
        ChooseGroupDialog(
            {
                showHiddenGroupsDialog = false
            },
            title = stringResource(R.string.hidden_groups),
            confirmFormatter = { stringResource(R.string.next) },
            infoFormatter = { it.seenText(context.getString(R.string.active), me) },
            groups = {
                api.hiddenGroups {
                    groups = it
                }
                allHiddenGroups = groups
                groups
            }
        ) { selected ->
            selectedHiddenGroups = selected
        }
    }

    if (showPushPermissionDialog) {
        RationaleDialog(
            {
                showPushPermissionDialog = false
            },
            stringResource(R.string.notifications_disabled_message)
        )
    }

    if (showCreateGroupName) {
        TextFieldDialog(
            onDismissRequest = { showCreateGroupName = false },
            title = stringResource(R.string.group_name),
            button = stringResource(R.string.next),
            singleLine = true,
            initialValue = createGroupName,
            placeholder = stringResource(R.string.empty_group_name),
            requireModification = false
        ) { value ->
            createGroupName = value
            showCreateGroupName = false
            showCreateGroupMembers = true
        }
    }

    if (showCreateGroupMembers) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            {
                showCreateGroupMembers = false
            },
            title = stringResource(R.string.invite_people),
            confirmFormatter = defaultConfirmFormatter(
                R.string.skip,
                R.string.new_group_with_person,
                R.string.new_group_with_people,
                R.string.new_group_with_x_people
            ) { it.name ?: someone },
            allowNone = true,
            onPeopleSelected = { people ->
                api.createGroup(people.map { it.id!! }) { group ->
                    if (createGroupName.isNotBlank()) {
                        api.updateGroup(group.id!!, Group(name = createGroupName))
                    }
                    nav.navigate("group/${group.id!!}")
                }
            },
            omit = { it.id == me?.id }
        )
    }
}

fun GroupExtended.seenText(active: String, me: Person?): String? {
    val otherMemberLastSeen = members?.filter { it.person?.id != me?.id }?.maxByOrNull {
        it.person?.seen ?: Instant.fromEpochMilliseconds(0)
    }?.person?.seen
    return otherMemberLastSeen?.timeAgo()?.lowercase()?.let { timeAgo ->
        "$active $timeAgo"
    }
}
