package com.queatz.ailaai.ui.screens

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.api.*
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.services.messages
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.components.SearchResult
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FriendsScreen(navController: NavController, me: () -> Person?) {
    val state = rememberLazyListState()
    var searchText by rememberSaveable { mutableStateOf("") }
    var allGroups by remember { mutableStateOf(emptyList<GroupExtended>()) }
    var allPeople by remember { mutableStateOf(emptyList<Person>()) }
    var results by remember { mutableStateOf(emptyList<SearchResult>()) }
    var isLoading by rememberStateOf(true)
    var createGroupName by remember { mutableStateOf("") }
    var showCreateGroupName by rememberStateOf(false)
    var showCreateGroupMembers by rememberStateOf(false)
    var showPushPermissionDialog by rememberStateOf(false)
    var showHiddenGroupsDialog by rememberStateOf(false)
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun update() {
        results = allPeople.map { SearchResult.Connect(it) } +
                searchText.trim().let { text ->
                    (if (text.isBlank()) allGroups else allGroups.filter {
                        (it.group?.name?.contains(text, true) ?: false) ||
                                it.members?.any {
                                    it.person?.id != me()?.id && it.person?.name?.contains(
                                        text,
                                        true
                                    ) ?: false
                                } ?: false
                    }).map { SearchResult.Group(it) }
                }
    }

    suspend fun reload(passive: Boolean) {
        isLoading = results.isEmpty()
        api.groups(
            onError = {
                if (!passive) {
                    context.showDidntWork()
                }
            }
        ) {
            allGroups = it.filter { it.group != null }
        }
        update()
        messages.refresh(me(), allGroups)
        isLoading = false
    }

    OnLifecycleEvent { event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                reload(passive = true)
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (!notificationManager.areNotificationsEnabled()) {
            if (!notificationPermissionState.status.isGranted) {
                if (notificationPermissionState.status.shouldShowRationale) {
                    showPushPermissionDialog = true
                } else {
                    notificationPermissionState.launchPermissionRequest()
                }
            }
        }
    }

    LaunchedEffect(searchText) {
        // todo search server, set allGroups
        if (searchText.isBlank()) {
            allPeople = emptyList()
        } else {
            api.people(searchText) {
                allPeople = it
                update()
            }
        }
        update()
    }

    Column {
        AppHeader(
            navController,
            if (allGroups.isEmpty()) stringResource(R.string.your_groups) else "${stringResource(R.string.your_groups)} (${allGroups.size})",
            {
                scope.launch {
                    state.scrollToTop()
                }
            },
            me,
            showAppIcon = true
        ) {
            var showMenu by rememberStateOf(false)
            IconButton(
                {
                    showMenu = true
                }
            ) {
                Icon(Icons.Outlined.MoreVert, null)
                DropdownMenu(showMenu, { showMenu = false }) {
                    DropdownMenuItem({
                        Text(stringResource(R.string.hidden_groups))
                    }, {
                        showMenu = false
                        showHiddenGroupsDialog = true
                    })
                }
            }
        }
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true
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
                } else if (results.isEmpty()) {
                    item {
                        Text(
                            stringResource(if (searchText.isBlank()) R.string.you_have_no_groups else R.string.no_groups_to_show),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(PaddingDefault * 2)
                        )
                    }
                } else {
                    items(results, key = {
                        when (it) {
                            is SearchResult.Connect -> "connect:${it.person.id}"
                            is SearchResult.Group -> "group:${it.groupExtended.group!!.id!!}"
                        }
                    }) {
                        ContactItem(navController, it, me()) {
                            scope.launch {
                                reload(true)
                            }
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PaddingDefault * 2)
                    .widthIn(max = 480.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth()
                ) {
                    SearchField(
                        searchText,
                        { searchText = it },
                        placeholder = stringResource(R.string.search_people_and_groups),
                    )
                }
                FloatingActionButton(
                    onClick = {
                        createGroupName = ""
                        showCreateGroupName = true
                    },
                    modifier = Modifier
                        .padding(start = PaddingDefault * 2)
                ) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.new_group))
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
            confirmFormatter = { stringResource(R.string.show) },
            infoFormatter = { it.seenText(context.getString(R.string.active)) },
            me = me(),
            groups = {
                api.hiddenGroups {
                    groups = it
                }
                groups
            }
        ) { selected ->
            coroutineScope {
                groups
                    .filter { group -> selected.any { it.id == group.group?.id } }
                    .mapNotNull { groupExtended ->
                        val member = groupExtended.members?.firstOrNull { it.person?.id == me()?.id }?.member
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
                reload(true)
            }
        }
    }

    if (showPushPermissionDialog) {
        AlertDialog(
            {
                showPushPermissionDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${navController.context.packageName}")
                        )
                        (navController.context as Activity).startActivity(intent)
                        showPushPermissionDialog = false
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            text = {
                Text(stringResource(R.string.notifications_disabled_message))
            }
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
                    navController.navigate("group/${group.id!!}")
                }
            },
            omit = { it.id == me()?.id }
        )
    }
}

fun GroupExtended.seenText(active: String) = group?.seen?.timeAgo()?.let { timeAgo ->
    "$active ${timeAgo.lowercase()}"
}
