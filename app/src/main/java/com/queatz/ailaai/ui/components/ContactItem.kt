package com.queatz.ailaai.ui.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import app.ailaai.api.createGroup
import app.ailaai.api.updateMember
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.services.joins
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.GroupExtended
import com.queatz.db.Member
import com.queatz.db.Message
import com.queatz.db.Person
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

enum class GroupInfo {
    LatestMessage,
    Members
}

@Composable
fun ContactItem(
    navController: NavController,
    item: SearchResult,
    me: Person?,
    onChange: () -> Unit,
    info: GroupInfo
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
                    ContactPhoto(item.person.name ?: "", item.person.photo, item.person.seen)
                )
            )
        }

        is SearchResult.Group -> {
            val joinRequestCount by joins.joins
                .map { it.count { it.joinRequest?.group == item.groupExtended.group?.id } }
                .collectAsState(0)
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

            val description = when (info) {
                GroupInfo.LatestMessage -> {
                    groupExtended.latestMessage?.preview(context)?.let {
                        if (groupExtended.latestMessage!!.member == myMember?.member?.id) stringResource(
                            R.string.you_x,
                            it
                        ) else it
                    } ?: stringResource(
                        if (people.size == 1) R.string.connected_ago else R.string.created_ago,
                        groupExtended.group!!.createdAt!!.timeAgo().lowercase()
                    )
                }

                GroupInfo.Members -> {
                    buildString {
                        append(groupExtended.members!!.size.format())
                        append(" ")
                        append(pluralStringResource(R.plurals.inline_members, groupExtended.members!!.size))
                        if (groupExtended.group?.description.isNullOrBlank().not()) {
                            append(" • ")
                            append(groupExtended.group!!.description)
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
                description = description,
                photos = groupExtended.photos(me?.let(::listOf) ?: emptyList(), ifEmpty = me?.let(::listOf)),
                lastActive = groupExtended.latestMessage?.createdAt?.timeAgo(),
                isUnread = isUnread || joinRequestCount > 0,
                joinRequestCount = joinRequestCount,
                joined = myMember != null,
                info = info
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
    isUnread: Boolean = false,
    joinRequestCount: Int = 0,
    joined: Boolean = false,
    info: GroupInfo = GroupInfo.LatestMessage
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
        val bold = isUnread || (info == GroupInfo.Members && joined)
        Text(
            if (info == GroupInfo.Members && joined) {
                stringResource(R.string.joined)
            } else if (joinRequestCount > 0) pluralStringResource(
                R.plurals.x_requests,
                joinRequestCount,
                joinRequestCount
            ) else lastActive ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .padding(PaddingDefault)
                .let {
                    if (bold) {
                        it
                    } else {
                        it.alpha(.5f)
                    }
                }
        )
        if (info == GroupInfo.Members && joined) {
            Icon(
                Icons.Outlined.Check,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = PaddingDefault)
            )
        }
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
