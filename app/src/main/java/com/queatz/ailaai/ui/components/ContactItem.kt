package com.queatz.ailaai.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.item
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@Composable
fun ContactItem(
    navController: NavController,
    item: SearchResult,
    me: Person?,
    onChange: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val someone = stringResource(R.string.someone)
    val notConnected = stringResource(R.string.not_connected_yet)

    when (item) {
        is SearchResult.Connect -> {
            ContactResult(
                onClick = {
                    scope.launch {
                        try {
                            val group = api.createGroup(listOf(me!!.id!!, item.person.id!!), reuse = true)
                            navController.navigate("group/${group.id!!}")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onLongClick = {},
                name = item.person.name ?: someone,
                description = notConnected,
                photos = listOf(
                    ContactPhoto(item.person.name ?: "", item.person.photo)
                )
            )
        }
        is SearchResult.Group -> {
            val groupExtended = item.groupExtended
            val people = groupExtended.members?.filter { it.person?.id != me?.id }?.map { it.person!! } ?: emptyList()
            val myMember = groupExtended.members?.find { it.person?.id == me?.id }
            val isUnread = groupExtended.isUnread(myMember?.member)
            var showMenu by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            if (showMenu) {
                Menu(
                    { showMenu = false }
                ) {
                    item(stringResource(R.string.hide)) {
                        coroutineScope.launch {
                            try {
                                api.updateMember(myMember!!.member!!.id!!, Member(hide = true))
                                Toast.makeText(
                                    navController.context,
                                    navController.context.getString(R.string.group_hidden),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onChange()
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                }
            }

            val emptyGroup = stringResource(R.string.empty_group_name)
            ContactResult(
                onClick = {
                    navController.navigate("group/${groupExtended.group!!.id!!}")
                },
                onLongClick = {
                    showMenu = true
                },
                name = groupExtended.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()),
                description = groupExtended.latestMessage?.text?.let {
                    if (groupExtended.latestMessage!!.member == myMember?.member?.id) stringResource(
                        R.string.you_x,
                        it
                    ) else it
                } ?: stringResource(
                    if (people.size == 1) R.string.connected_ago else R.string.created_ago,
                    groupExtended.group!!.createdAt!!.timeAgo().lowercase()
                ),
                photos = groupExtended.photos(me?.let(::listOf) ?: emptyList()),
                lastActive = groupExtended.latestMessage?.createdAt?.timeAgo(),
                isUnread = isUnread
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactResult(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    name: String,
    description: String,
    photos: List<ContactPhoto>,
    lastActive: String? = null,
    isUnread: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onLongClick = {
                    onLongClick()
                }
            ) {
                onClick()
            }
    ) {
        GroupPhoto(photos)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            lastActive ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .padding(PaddingDefault)
                .let {
                    if (isUnread) {
                        it
                    } else {
                        it.alpha(.5f)
                    }
                }
        )
    }
}

sealed class SearchResult {
    class Connect(val person: Person) : SearchResult()
    class Group(val groupExtended: GroupExtended) : SearchResult()
}
