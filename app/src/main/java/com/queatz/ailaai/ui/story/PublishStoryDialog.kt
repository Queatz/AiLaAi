package com.queatz.ailaai.ui.story

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.api.card
import com.queatz.ailaai.api.profile
import com.queatz.ailaai.api.storyDraft
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.SetLocationDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.http.*

@Composable
fun PublishStoryDialog(
    onDismissRequest: () -> Unit,
    activity: Activity,
    story: Story,
    storyContents: List<StoryContent>,
    me: () -> Person?,
    onLocationChanged: (LatLng?) -> Unit,
    onGroupsChanged: (List<Group>) -> Unit,
    onPublish: () -> Unit,
) {
    val context = LocalContext.current
    var publishEnabled by rememberStateOf(false)
    var showLocationDialog by rememberStateOf(false)
    var showGroupsDialog by rememberStateOf(false)
    var shareToGroups by rememberStateOf<List<Group>?>(null)
    var geo by rememberStateOf(story.geo?.toLatLng())
    var shareToGeo by rememberStateOf(story.geo?.toLatLng())
    var containsCards by rememberStateOf<Boolean?>(null)
    var allCardsArePublished by rememberStateOf<Boolean?>(null)
    var friendCount by rememberStateOf(0)
    var storyDraft by rememberStateOf<StoryDraft?>(null)
    val locationSelector = locationSelector(
        geo,
        { geo = it },
        activity
    )

    LaunchedEffect(Unit) {
        locationSelector.start()
    }

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

    LaunchedEffect(me()) {
        me()?.id?.let { me ->
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
                && storyContents.sumOf { it.wordCount() } >= storyMinimumWordCount
                && allCardsArePublished == true
    }

    AlertDialog(
        onDismissRequest,
        properties = DialogProperties(
            decorFitsSystemWindows = Build.VERSION.SDK_INT < Build.VERSION_CODES.S, // Dialogs missing scrim
            usePlatformDefaultWidth = false
        ),
        title = {
            Text(stringResource(R.string.publish_story))
        },
        text = {
            val dialogScroll = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingDefault / 2),
                modifier = Modifier
                    .verticalScroll(dialogScroll)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val wordCount = storyContents.sumOf { it.wordCount() }
                    if (wordCount >= storyMinimumWordCount) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text(pluralStringResource(R.plurals.x_words, wordCount, wordCount))
                    } else {
                        Text(
                            text = stringResource(R.string.minimum_words_count, wordCount),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .padding(bottom = PaddingDefault / 2)
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.shapes.medium
                                )
                                .padding(PaddingDefault)
                        )
                    }
                }
                if (containsCards == true) {
                    allCardsArePublished?.let { allCardsArePublished ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (allCardsArePublished) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Text(stringResource(R.string.all_cards_are_published))
                            } else {
                                Text(
                                    text = stringResource(R.string.story_contains_draft_cards),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .padding(bottom = PaddingDefault / 2)
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                                            MaterialTheme.shapes.medium
                                        )
                                        .padding(PaddingDefault)
                                )
                            }
                        }
                    }
                }
                if (friendCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
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
                            .padding(vertical = PaddingDefault / 2)
                    ) {
                        val peopleNearby = stringResource(R.string.inline_people_nearby)
                        Icon(
                            Icons.Outlined.Place,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = PaddingDefault)
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
                            Icons.Outlined.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = PaddingDefault / 2)
                                .size(16.dp)
                        )
                    }
                }
                if (!shareToGroups.isNullOrEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = if (shareToGroups?.size == 1) Alignment.CenterVertically else Alignment.Top,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                showGroupsDialog = true
                            }
                            .padding(vertical = PaddingDefault / 2)
                    ) {
                        Icon(
                            Icons.Outlined.Forum,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = PaddingDefault)
                        )
                        Text(
                            buildAnnotatedString {
                                append(stringResource(R.string.shared_in_))
                                append(" ")
                                shareToGroups?.forEachIndexed { index, group ->
                                    if (index > 0) append(", ")
                                    bold {
                                        append(group.name ?: "")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f, fill = false)
                        )
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = PaddingDefault / 2)
                                .size(16.dp)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.published_stories_cant_be_edited))
                }

                if (shareToGeo == null) {
                    OutlinedButton(
                        onClick = {
                            showLocationDialog = true
                        }
                    ) {
                        Icon(Icons.Outlined.Place, null, modifier = Modifier.padding(end = PaddingDefault))
                        Text(stringResource(R.string.add_a_location))
                    }
                    Text(
                        stringResource(R.string.add_a_location_description),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (shareToGroups.isNullOrEmpty()) {
                    OutlinedButton(
                        onClick = {
                            showGroupsDialog = true
                        }
                    ) {
                        Icon(Icons.Outlined.GroupAdd, null, modifier = Modifier.padding(end = PaddingDefault))
                        Text(stringResource(R.string.share_to_groups))
                    }
                    Text(
                        stringResource(R.string.share_to_groups_description),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

//                OutlinedButton(
//                    onClick = {
//
//                    }
//                ) {
//                    Icon(Icons.Outlined.GroupAdd, null, modifier = Modifier.padding(end = PaddingDefault))
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
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.close))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPublish()
                },
                enabled = publishEnabled
            ) {
                Text(stringResource(R.string.publish))
            }
        },
        modifier = Modifier
            .padding(PaddingDefault * 2)
            .imePadding()
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

    if (showGroupsDialog) {
        val someone = stringResource(R.string.someone)
        val emptyGroup = stringResource(R.string.empty_group_name)
        ChooseGroupDialog(
            {
                showGroupsDialog = false
            },
            title = stringResource(R.string.share),
            confirmFormatter = defaultConfirmFormatter(
                R.string.choose_none,
                R.string.choose_x,
                R.string.choose_x_and_x,
                R.string.choose_x_groups
            ) { it.name(someone, emptyGroup, omit = me()?.id?.let(::listOf) ?: emptyList()) },
            filter = { it.isGroupLike() },
            allowNone = true,
            preselect = shareToGroups,
            me = me()
        ) {
            shareToGroups = it
            onGroupsChanged(it)
        }
    }
}

const val storyMinimumWordCount = 25
