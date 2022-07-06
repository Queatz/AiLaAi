package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.queatz.ailaai.GroupExtended
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlin.random.Random

@Composable
fun ContactItem(navController: NavController, groupExtended: GroupExtended) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .clip(MaterialTheme.shapes.large)
        .clickable {
            navController.navigate("group/${groupExtended.group!!.id!!}")
        }) {
        AsyncImage(
            model = "https://minimaltoolkit.com/images/randomdata/${
                listOf("female", "male").random(Random(groupExtended.group!!.id!!.hashCode()))
            }/${Random(groupExtended.group!!.id!!.hashCode()).nextInt(1, 100)}.jpg",
            "",
            Modifier
                .padding(PaddingDefault)
                .requiredSize(64.dp)
                .clip(CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                groupExtended.members?.firstOrNull()?.person?.name ?: "Someone",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                groupExtended.latestMessage?.text ?: "",
                style = MaterialTheme.typography.bodyMedium,
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
