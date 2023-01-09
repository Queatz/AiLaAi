package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.queatz.ailaai.GroupExtended
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
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
        if (people.size == 1) {
            AsyncImage(
                model = people.firstOrNull()?.photo?.let { api.url(it) } ?: "",
                contentDescription = "",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .padding(PaddingDefault)
                    .requiredSize(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
        } else if (people.size >= 2) {
            val show = remember { people.shuffled() }
            Box(modifier = Modifier.requiredSize(64.dp)) {
                AsyncImage(
                    model = show[0].photo?.let { api.url(it) } ?: "",
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaddingDefault)
                        .requiredSize(32.dp)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                )
                AsyncImage(
                    model = show[1].photo?.let { api.url(it) } ?: "",
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PaddingDefault)
                        .requiredSize(32.dp)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val someone = stringResource(R.string.someone)
            Text(
                people.joinToString { it.name ?: someone },
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
