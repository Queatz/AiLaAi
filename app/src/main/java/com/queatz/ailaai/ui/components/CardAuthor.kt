package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.timeAgo

@Composable
fun CardAuthor(person: Person, navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (person.name?.isNotBlank() == true || person.photo?.isNotBlank() == true) {
            GroupPhoto(
                listOf(ContactPhoto(person.name ?: "", person.photo)),
                size = 24.dp,
                padding = 0.dp,
                modifier = Modifier
                    .clickable {
                        navController.navigate("profile/${person.id}")
                    }
            )
        } else {
            IconButton({
                navController.navigate("profile/${person.id}")
            }) {
                Icon(Icons.Outlined.AccountCircle, Icons.Outlined.Settings.name)
            }
        }
        Column {
            Text(person.name ?: stringResource(R.string.someone), modifier = Modifier.clickable(
                MutableInteractionSource(),
                null
            ) {
                navController.navigate("profile/${person.id}")
            }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(person.seen?.timeAgo()?.let { timeAgo ->
                "${context.getString(R.string.active)} ${timeAgo.lowercase()}"
            } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
