package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ailaai.api.activeGroupInvites
import app.ailaai.api.deleteInvite
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.formatFuture
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Invite
import kotlinx.coroutines.launch

@Composable
fun ActiveInvitesDialog(
    onDismissRequest: () -> Unit,
    groupId: String
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var invites by remember { mutableStateOf<List<Invite>?>(null) }
    var isLoading by remember(groupId) { mutableStateOf(true) }
    var showCreateInviteDialog by remember { mutableStateOf(false) }
    var inviteToDelete by remember { mutableStateOf<Invite?>(null) }

    suspend fun reload() {
        api.activeGroupInvites(groupId, onError = {
            isLoading = false
            context.showDidntWork()
        }) { result ->
            invites = result
            isLoading = false
        }
    }

    // Load active invites
    LaunchedEffect(groupId) {
        reload()
    }

    if (showCreateInviteDialog) {
        CreateInviteDialog(
            onDismissRequest = {
                showCreateInviteDialog = false
            },
            groupId = groupId,
            onInviteCreated = {
                context.toast(context.getString(R.string.invite_created))
                scope.launch {
                    reload()
                }
                showCreateInviteDialog = false
            }
        )
    }

    if (inviteToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                inviteToDelete = null
            },
            title = {
                Text(stringResource(R.string.delete_invite))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val invite = inviteToDelete
                        inviteToDelete = null
                        if (invite != null) {
                            scope.launch {
                                api.deleteInvite(invite.id!!) {
                                    reload()
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.yes_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        inviteToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    DialogBase(onDismissRequest) {
        DialogLayout(
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.active_invites),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 1.pad)
                    )
                    IconButton(
                        onClick = {
                            showCreateInviteDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLink,
                            contentDescription = stringResource(R.string.create_invite)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                ) {
                    if (isLoading) {
                        Loading()
                    } else if (invites.isNullOrEmpty()) {
                        EmptyText(
                            stringResource(R.string.no_active_invites),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(1.pad),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp)
                        ) {
                            items(invites!!) { invite ->
                                InviteItem(
                                    invite = invite,
                                    onDelete = {
                                        inviteToDelete = invite
                                    }
                                )
                            }
                        }
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
            }
        )
    }
}

@Composable
fun InviteItem(
    invite: Invite,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(1.5f.pad)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (invite.about != null) {
                        Text(
                            invite.about!!,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Show remaining uses
                    Text(
                        if (invite.total == null) {
                            stringResource(R.string.unlimited_uses)
                        }
                        else {
                            stringResource(R.string.x_of_y_uses_remaining, invite.total!!, invite.total!!)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Show expiration if applicable
                    if (invite.expiry != null) {
                        Text(
                            stringResource(R.string.expires_x, invite.expiry!!.formatFuture()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = stringResource(R.string.delete_invite)
                    )
                }
            }
        }
    }
}
