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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person

@Composable
fun PersonItem(person: Person, interactable: Boolean = true) {
    val nav = nav
    val context = LocalContext.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .let {
                if (interactable) {
                    it.clickable(
                        remember { MutableInteractionSource() },
                        null
                    ) {
                        nav.navigate("profile/${person.id}")
                    }
                } else {
                    it
                }
            }
    ) {
        if (person.name?.isNotBlank() == true || person.photo?.isNotBlank() == true) {
            GroupPhoto(
                listOf(ContactPhoto(person.name ?: "", person.photo, person.seen)),
                size = 28.dp,
                padding = 0.dp,
                modifier = Modifier
                    .let {
                        if (interactable) {
                            it.clickable(
                                remember { MutableInteractionSource() },
                                null
                            ) {
                                nav.navigate("profile/${person.id}")
                            }
                        } else {
                            it
                        }
                    }
            )
        } else {
            IconButton({
                nav.navigate("profile/${person.id}")
            }) {
                Icon(Icons.Outlined.AccountCircle, Icons.Outlined.Settings.name)
            }
        }
        Column {
            Text(
                person.name ?: stringResource(R.string.someone),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(person.seen?.timeAgo()?.let { timeAgo ->
                "${context.getString(R.string.active)} ${timeAgo.lowercase()}"
            } ?: "",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = .5f)
            )
        }
    }
}
