package com.queatz.ailaai.ui.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.queatz.ailaai.*
import com.queatz.ailaai.api.createGroup
import com.queatz.ailaai.api.updateMember
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Composable
fun ContactItem(
    navController: NavController,
    item: SearchResult,
    me: Person?,
    onChange: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val someone = stringResource(R.string.someone)
    val notConnected = stringResource(R.string.not_connected_yet)

    when (item) {
        is SearchResult.Connect -> {
            ContactResult(
                onClick = {
                    scope.launch {
                        api.createGroup(listOf(me!!.id!!, item.person.id!!), reuse = true) { group ->
                            navController.navigate("group/${group.id!!}")
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
            var showMenu by rememberStateOf(false)

            if (showMenu) {
                Menu(
                    { showMenu = false }
                ) {
                    menuItem(stringResource(R.string.hide)) {
                        showMenu = false
                        scope.launch {
                            api.updateMember(myMember!!.member!!.id!!, Member(hide = true)) {
                                context.toast(R.string.group_hidden)
                                onChange()
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
                description = groupExtended.latestMessage?.preview(context)?.let {
                    if (groupExtended.latestMessage!!.member == myMember?.member?.id) stringResource(
                        R.string.you_x,
                        it
                    ) else it
                } ?: stringResource(
                    if (people.size == 1) R.string.connected_ago else R.string.created_ago,
                    groupExtended.group!!.createdAt!!.timeAgo().lowercase()
                ),
                photos = groupExtended.photos(me?.let(::listOf) ?: emptyList(), ifEmpty = me?.let(::listOf)),
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

private fun Message.preview(context: Context): String? {
    return text?.nullIfBlank ?: attachmentText(context)
}


@Serializable
sealed class SearchResult {
    @Serializable
    class Connect(val person: Person) : SearchResult()
    @Serializable
    class Group(val groupExtended: GroupExtended) : SearchResult()
}
