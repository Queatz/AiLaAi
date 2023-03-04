package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.queatz.ailaai.GroupExtended
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.photos
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun ContactItem(navController: NavController, groupExtended: GroupExtended, me: Person?) {
    val people = groupExtended.members?.filter { it.person?.id != me?.id }?.map { it.person!! } ?: emptyList()
    val myMember = groupExtended.members?.find { it.person?.id == me?.id }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .clip(MaterialTheme.shapes.large)
        .clickable {
            navController.navigate("group/${groupExtended.group!!.id!!}")
        }) {
        GroupPhoto(groupExtended.photos(me?.let(::listOf) ?: emptyList()))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val someone = stringResource(R.string.someone)
            Text(
                groupExtended.name(someone, me?.id?.let(::listOf) ?: emptyList()),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                groupExtended.latestMessage?.text?.let {
                    if (groupExtended.latestMessage!!.member == myMember?.member?.id) stringResource(
                        R.string.you_x,
                        it
                    ) else it
                } ?: stringResource(if (people.size == 1) R.string.connected_ago else R.string.created_ago, groupExtended.group!!.createdAt!!.timeAgo().lowercase()),
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
            modifier = Modifier.padding(PaddingDefault)
        )
    }
}
