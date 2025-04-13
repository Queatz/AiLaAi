package com.queatz.ailaai.ui.story

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.ailaai.api.card
import app.ailaai.api.profile
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.api.storyDraft
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.bold
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.status
import com.queatz.ailaai.extensions.toLatLng
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.ChooseGroups
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.dialogs.SetLocationDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.Group
import com.queatz.db.Story
import com.queatz.db.StoryContent
import com.queatz.db.StoryDraft
import io.ktor.http.HttpStatusCode

@Composable
fun PublishStoryDialog(
    onDismissRequest: () -> Unit,
    activity: Activity,
    story: Story,
    storyContents: List<StoryContent>,
    onLocationChanged: (LatLng?) -> Unit,
    onGroupsChanged: (List<Group>) -> Unit,
    onPublish: () -> Unit,
) {
    val context = LocalContext.current
    var publishEnabled by rememberStateOf(false)
    var showLocationDialog by rememberStateOf(false)
    var shareToGroups by rememberStateOf<List<Group>?>(null)
    var geo by rememberStateOf(story.geo?.toLatLng())
    var shareToGeo by rememberStateOf(story.geo?.toLatLng())
    var containsCards by rememberStateOf<Boolean?>(null)
    var allCardsArePublished by rememberStateOf<Boolean?>(null)
    var friendCount by rememberStateOf(0)
    var storyDraft by rememberStateOf<StoryDraft?>(null)
    val locationSelector = locationSelector(
        geo = geo,
        onGeoChange = { geo = it },
        activity = activity
    )
    val me = me

    LaunchedEffect(Unit) {
        api.storyDraft(story.id!!, onError = {
            shareToGroups = emptyList()
            if (it.status == HttpStatusCode.NotFound) {
                // Ignored
            } else {
                context.showDidntWork()
            }
        }) {
            storyDraft = it
            shareToGroups = it.groupDetails ?: emptyList()
        }
        api.storyDraft(story.id!!, onError = {
            shareToGroups = emptyList()
            if (it.status == HttpStatusCode.NotFound) {
                // Ignored
            } else {
                context.showDidntWork()
            }
        }) {
            storyDraft = it
            shareToGroups = it.groupDetails ?: emptyList()
        }
    }

    LaunchedEffect(me) {
        me?.id?.let { me ->
            api.profile(me) {
                friendCount = it.stats.friendsCount
            }
        }
    }

    LaunchedEffect(Unit) {
        allCardsArePublished = storyContents
            .mapNotNull { it as? StoryContent.Cards }
            .flatMap {
                it.cards.map {
                    var card: Card? = null
                    api.card(it) {
                        card = it
                    }
                    card
                }
            }.also {
                containsCards = it.isNotEmpty()
            }
            .all { it?.active == true }
    }

    LaunchedEffect(story, storyContents, allCardsArePublished) {
        publishEnabled = !story.title.isNullOrBlank()
                && storyContents.any { it.isNotBlank() }
                && allCardsArePublished == true
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            decorFitsSystemWindows = Build.VERSION.SDK_INT < Build.VERSION_CODES.S, // Dialogs missing scrim
            usePlatformDefaultWidth = false
        ),
        text = {
            val dialogScroll = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(.5f.pad),
                modifier = Modifier
                    .verticalScroll(dialogScroll)
            ) {
                if (containsCards == true) {
                    allCardsArePublished?.let { allCardsArePublished ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.pad),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (allCardsArePublished) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Text(stringResource(R.string.all_cards_are_posted))
                            } else {
                                Text(
                                    text = stringResource(R.string.story_contains_unposted_cards),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .padding(bottom = .5f.pad)
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                                            MaterialTheme.shapes.medium
                                        )
                                        .padding(1.pad)
                                )
                            }
                        }
                    }
                }
                if (friendCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.pad),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Group, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        val numberOfFriends = pluralStringResource(R.plurals.x_friends, friendCount, friendCount)
                        Text(
                            buildAnnotatedString {
                                append(stringResource(R.string.shared_with_your_))
                                append(" ")
                                bold {
                                    append(numberOfFriends)
                                }
                            }
                        )
                    }
                }
                if (shareToGeo != null) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                shareToGeo = null
                                onLocationChanged(null)
                            }
                            .padding(vertical = .5f.pad)
                    ) {
                        val peopleNearby = stringResource(R.string.inline_people_nearby)
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 1.pad)
                        )
                        Text(
                            buildAnnotatedString {
                                append(stringResource(R.string.shared_with_))
                                append(" ")
                                bold {
                                    append(peopleNearby)
                                }
                            }
                        )
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = .5f.pad)
                                .size(16.dp)
                        )
                    }
                }

                ChooseGroups(
                    groups = shareToGroups.orEmpty(),
                    hint = stringResource(R.string.share_to_groups_description)
                ) {
                    shareToGroups = it
                    onGroupsChanged(it)
                }

                if (shareToGeo == null) {
                    OutlinedButton(
                        onClick = {
                            showLocationDialog = true
                        }
                    ) {
                        Icon(Icons.Outlined.Place, null, modifier = Modifier.padding(end = 1.pad))
                        Text(stringResource(R.string.add_a_location))
                    }
                    Text(
                        text = stringResource(R.string.add_a_location_description),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

//                OutlinedButton(
//                    onClick = {
//
//                    }
//                ) {
//                    Icon(Icons.Outlined.GroupAdd, null, modifier = Modifier.padding(end = 1.pad))
//                    Text("Add collaborators")
//                }
//                Text(
//                    """
//                    Adding co-authors will make your story visible to their friends.
//                    """.trimIndent(),
//                    color = MaterialTheme.colorScheme.secondary,
//                    style = MaterialTheme.typography.bodySmall
//                )
            }
        },
        dismissButton = {
            DialogCloseButton(onDismissRequest)
        },
        confirmButton = {
            Button(
                onClick = {
                    onPublish()
                },
                enabled = publishEnabled
            ) {
                Text(stringResource(R.string.post))
            }
        },
        modifier = Modifier
            .padding(2.pad)
            .imePadding() // todo Use DialogBase
    )

    if (showLocationDialog) {
        SetLocationDialog(
            {
                showLocationDialog = false
            },
            initialLocation = geo ?: LatLng(0.0, 0.0),
            initialZoom = geo?.let { 14f } ?: 5f,
        ) {
            geo = it
            shareToGeo = it
            onLocationChanged(it)
        }
    }
}
