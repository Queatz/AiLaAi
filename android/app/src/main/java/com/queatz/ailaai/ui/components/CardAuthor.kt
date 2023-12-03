package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person

@Composable
fun CardAuthor(
    people: List<Person>,
    interactable: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var viewport by rememberStateOf(Size(0f, 0f))
    val scrollState = rememberLazyListState()
    val nav = nav

    LazyRow(
        state = scrollState,
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier
            .fillMaxWidth()
            .onPlaced { viewport = it.boundsInParent().size }
            .horizontalFadingEdge(viewport, scrollState)
    ) {
        items(people) { person ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .let {
                        if (interactable) {
                            it.clickable(
                                MutableInteractionSource(),
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
                                        MutableInteractionSource(),
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
    }
}
