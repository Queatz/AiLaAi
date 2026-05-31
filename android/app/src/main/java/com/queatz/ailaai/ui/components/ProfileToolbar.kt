package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ConnectWithoutContact
import androidx.compose.material.icons.outlined.Rocket
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.ailaai.api.hiddenGroups
import app.ailaai.api.removeMember
import app.ailaai.api.updateMember
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.BuildConfig
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.impromptu.ImpromptuDialog
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.connectivity
import com.queatz.ailaai.services.ui
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.scripts.PreviewScriptAction
import com.queatz.ailaai.ui.scripts.ScriptsDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.Member
import com.queatz.db.Person
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Instant

@Composable
fun ProfileToolbar() {
    val me = me
    val nav = nav
    val scope = rememberCoroutineScope()
    var showScriptsDialog by rememberStateOf(false)
    var showImpromptuDialog by rememberStateOf(false)
    var showHiddenGroupsDialog by rememberStateOf(false)
    var allHiddenGroups by remember { mutableStateOf(emptyList<GroupExtended>()) }
    var selectedHiddenGroups by rememberStateOf(listOf<Group>())
    val hasConnectivity = connectivity.hasConnectivity

    if (showImpromptuDialog) {
        ImpromptuDialog(
            onDismissRequest = {
                showImpromptuDialog = false
            }
        )
    }

    if (showScriptsDialog) {
        ScriptsDialog(
            onDismissRequest = {
                showScriptsDialog = false
            },
            previewScriptAction = PreviewScriptAction.Edit
        )
    }

    if (showHiddenGroupsDialog) {
        var groups by rememberStateOf(listOf<GroupExtended>())
        ChooseGroupDialog(
            {
                showHiddenGroupsDialog = false
            },
            title = stringResource(R.string.hidden_groups),
            confirmFormatter = { stringResource(R.string.next) },
            infoFormatter = {
                val activeString = stringResource(R.string.active)
                it.seenText(activeString, me)
            },
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

    if (selectedHiddenGroups.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                selectedHiddenGroups = emptyList()
            },
            title = {
                Text(
                    pluralStringResource(
                        R.plurals.x_groups,
                        selectedHiddenGroups.size,
                        selectedHiddenGroups.size
                    )
                )
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
                    onClick = {
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
                                selectedHiddenGroups = emptyList()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.show))
                }
            })
    }

    Toolbar(outline = true) {
            val showOfflineNote by ui.showOfflineNote.collectAsState()
            item(
                icon = Icons.Outlined.Rocket,
                name = stringResource(R.string.inventory),
            ) {
                nav.appNavigate(AppNav.Inventory)
            }
            item(
                icon = Icons.Outlined.VisibilityOff,
                name = stringResource(R.string.hidden_groups),
            ) {
                showHiddenGroupsDialog = true
            }
            ScanQrCodeButton(
                button = { onClick ->
                    item(
                        icon = Icons.Outlined.QrCodeScanner,
                        name = stringResource(R.string.scan),
                        onClick = onClick
                    )
                }
            ) {
            }
            if (BuildConfig.ENABLE_BACKGROUND_LOCATION) {
                item(
                    icon = Icons.Outlined.ConnectWithoutContact,
                    name = stringResource(R.string.impromptu_mode),
                ) {
                    showImpromptuDialog = true
                }
            }
            item(
                icon = Icons.Outlined.HistoryEdu,
                name = stringResource(R.string.scripts),
            ) {
                showScriptsDialog = true
            }
            item(
                icon = if (hasConnectivity) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                name = if (!showOfflineNote) {
                    stringResource(R.string.show_offline_notes)
                } else {
                    stringResource(R.string.hide_offline_notes)
                },
            ) {
                ui.showOfflineNote(!showOfflineNote)
            }
            item(
                icon = Icons.Outlined.Settings,
                name = stringResource(R.string.settings),
            ) {
                nav.appNavigate(AppNav.Settings)
            }
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
