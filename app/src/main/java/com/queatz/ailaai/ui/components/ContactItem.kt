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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.isUnread
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.photos
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.item
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(navController: NavController, groupExtended: GroupExtended, me: Person?, onChange: () -> Unit) {
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

    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onLongClick = {
                    showMenu = true
                }
            ) {
                navController.navigate("group/${groupExtended.group!!.id!!}")
            }
    ) {
        GroupPhoto(groupExtended.photos(me?.let(::listOf) ?: emptyList()))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val someone = stringResource(R.string.someone)
            val emptyGroup = stringResource(R.string.empty_group_name)
            Text(
                groupExtended.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                groupExtended.latestMessage?.text?.let {
                    if (groupExtended.latestMessage!!.member == myMember?.member?.id) stringResource(
                        R.string.you_x,
                        it
                    ) else it
                } ?: stringResource(
                    if (people.size == 1) R.string.connected_ago else R.string.created_ago,
                    groupExtended.group!!.createdAt!!.timeAgo().lowercase()
                ),
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            groupExtended.latestMessage?.createdAt?.timeAgo() ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(PaddingDefault)
        )
    }
}
