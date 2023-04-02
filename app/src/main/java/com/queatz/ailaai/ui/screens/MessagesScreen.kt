package com.queatz.ailaai.ui.screens

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.state.gsonSaver
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.utils.io.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MessagesScreen(navController: NavController, me: () -> Person?) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var allGroups by rememberSaveable(stateSaver = gsonSaver<List<GroupExtended>>()) { mutableStateOf(listOf()) }
    var groups by rememberSaveable(stateSaver = gsonSaver<List<GroupExtended>>()) { mutableStateOf(listOf()) }
    var isLoading by remember { mutableStateOf(groups.isEmpty()) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var showPushPermissionDialog by remember { mutableStateOf(false) }
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val context = LocalContext.current

    fun update() {
        groups = if (searchText.isBlank()) allGroups else allGroups.filter {
            (it.group?.name?.contains(searchText, true) ?: false) ||
                    it.members?.any { it.person?.name?.contains(searchText, true) ?: false } ?: false
        }
    }

    LaunchedEffect(Unit) {
        // Reload, but only show loading indicator when there are no groups
        isLoading = groups.isEmpty()
        try {
            allGroups = api.groups().filter { it.group != null }
            messages.refresh(me(), allGroups)
            update()
            isLoading = false
        } catch (ex: Exception) {
            if (ex is CancellationException || ex is InterruptedException) {
                // Ignore
            } else {
                ex.printStackTrace()
                isLoading = false
            }
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
        update()
    }

    Column {
        TopAppBar(
            {
                Text(stringResource(R.string.your_groups), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            actions = {
                ElevatedButton(
                    {
                        showCreateGroup = true
                    },
                    modifier = Modifier.padding(horizontal = PaddingDefault)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        stringResource(R.string.new_group),
                        modifier = Modifier.padding(end = PaddingDefault)
                    )
                    Text(stringResource(R.string.new_group))
                }
            }
        )
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
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
                } else if (groups.isEmpty()) {
                    item {
                        Text(
                            stringResource(if (searchText.isBlank()) R.string.you_have_no_groups else R.string.no_groups_to_show),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(PaddingDefault * 2)
                        )
                    }
                } else {
                    items(groups, key = { it.group!!.id!! }) {
                        ContactItem(navController, it, me())
                    }
                }
            }
            SearchField(
                searchText, { searchText = it }, modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PaddingDefault * 2)
            )
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
                Text("Message notifications are disabled")
            }
        )
    }

    if (showCreateGroup) {
        val context = LocalContext.current
        val didntWork = stringResource(R.string.didnt_work)
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            {
                showCreateGroup = false
            },
            title = stringResource(R.string.invite_people),
            confirmFormatter = defaultConfirmFormatter(
                R.string.new_group,
                R.string.new_group_with_person,
                R.string.new_group_with_people,
                R.string.new_group_with_x_people
            ) { it.name ?: someone },
            allowNone = true,
            onPeopleSelected = { people ->
                try {
                    val group = api.createGroup(people.map { it.id!! })
                    navController.navigate("group/${group.id!!}")
                } catch (ex: Exception) {
                    Toast.makeText(context, didntWork, Toast.LENGTH_SHORT).show()
                    ex.printStackTrace()
                }
            },
            omit = { it.id == me()?.id }
        )
    }
}
