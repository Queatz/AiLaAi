package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val person = groupExtended.members?.find { it.person?.id != me?.id }?.person

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .clip(MaterialTheme.shapes.large)
        .clickable {
            navController.navigate("group/${groupExtended.group!!.id!!}")
        }) {
        AsyncImage(
            model = person?.photo?.let { api.url(it) } ?: "",
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .padding(PaddingDefault)
                .requiredSize(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                person?.name ?: stringResource(R.string.someone),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                groupExtended.latestMessage?.text ?: "${stringResource(R.string.connected)} ${groupExtended.group!!.createdAt!!.timeAgo().lowercase()}",
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
