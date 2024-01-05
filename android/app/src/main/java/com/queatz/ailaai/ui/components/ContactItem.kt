package com.queatz.ailaai.ui.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ailaai.api.createGroup
import app.ailaai.api.updateMember
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.joins
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.theme.pad
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
    item: SearchResult,
    onChange: () -> Unit,
    info: GroupInfo,
    coverPhoto: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me = me
    val nav = nav
    var showMenu by rememberStateOf(false)

    if (showMenu) {
        Menu(
            { showMenu = false }
        ) {
            val groupExtended = (item as? SearchResult.Group)?.groupExtended
            if (groupExtended != null) {
                menuItem(stringResource(R.string.hide)) {
                    showMenu = false
                    scope.launch {
                        val myMember = groupExtended.members?.find { it.person?.id == me?.id }
                        api.updateMember(myMember!!.member!!.id!!, Member(hide = true)) {
                            context.toast(R.string.group_hidden)
                            onChange()
                        }
                    }
                }
            }
        }
    }

    ContactItem(
        {
            scope.launch {
                when (item) {
                    is SearchResult.Connect -> {
                        api.createGroup(listOf(me!!.id!!, item.person.id!!), reuse = true) { group ->
                            nav.navigate("group/${group.id!!}")
                        }
                    }

                    is SearchResult.Group -> {
                        val groupExtended = item.groupExtended
                        nav.navigate("group/${groupExtended.group!!.id!!}")

                    }
                }
            }
        },
        {
            if (item is SearchResult.Group) {
                showMenu = true
            }
        },
        item,
        info,
        coverPhoto
    )
}

@Composable
fun ContactItem(
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    item: SearchResult,
    info: GroupInfo,
    coverPhoto: Boolean = false
) {
    val context = LocalContext.current
    val someone = stringResource(R.string.someone)
    val notConnected = stringResource(R.string.not_connected_yet)

    when (item) {
        is SearchResult.Connect -> {
            ContactResult(
                onClick = onClick,
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
                onClick = onClick,
                onLongClick = onLongClick,
                name = groupExtended.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()),
                description = description,
                photos = groupExtended.photos(me?.let(::listOf) ?: emptyList(), ifEmpty = me?.let(::listOf)),
                lastActive = groupExtended.latestMessage?.createdAt?.timeAgo(),
                isUnread = isUnread || joinRequestCount > 0,
                joinRequestCount = joinRequestCount,
                joined = myMember != null,
                info = info,
                coverPhoto = if (coverPhoto) groupExtended.group?.photo?.let(api::url) else null
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactResult(
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    name: String,
    description: String,
    photos: List<ContactPhoto>,
    lastActive: String? = null,
    isUnread: Boolean = false,
    joinRequestCount: Int = 0,
    joined: Boolean = false,
    info: GroupInfo = GroupInfo.LatestMessage,
    coverPhoto: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .let {
                if (onClick == null && onLongClick == null) {
                    it
                } else {
                    it.combinedClickable(
                        onLongClick = onLongClick,
                        onClick = {
                            onClick?.invoke()
                        }
                    )
                }
            }
            .let {
                if (coverPhoto == null) {
                    it
                } else {
                    it
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                }
            }
    ) {
        if (coverPhoto != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverPhoto)
                    .crossfade(true)
                    .build(),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(
                        RoundedCornerShape(
                            MaterialTheme.shapes.large.topStart,
                            MaterialTheme.shapes.large.topEnd,
                            CornerSize(0.dp),
                            CornerSize(0.dp)
                        )
                    )
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            GroupPhoto(photos)
            Column(
                modifier = Modifier.weight(1f).let {
                    if (coverPhoto == null) {
                        it
                    } else {
                        it.padding(vertical = 1.pad)
                    }
                }
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
                    .padding(1.pad)
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
                        .padding(end = 1.pad)
                )
            }
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
