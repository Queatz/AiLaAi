package com.queatz.ailaai.ui.story

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api.*
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.ChooseCardDialog
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.Person
import kotlinx.coroutines.launch

@Composable
fun StoryCreatorTools(
    source: StorySource,
    navController: NavController,
    me: () -> Person?,
    addPart: (part: StoryContent) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCardSelectorDialog by rememberStateOf(false)
    var showCardGroupSelectorDialog by rememberStateOf(false)

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) {
        if (it.isEmpty()) return@rememberLauncherForActivityResult

        scope.launch {
            when (source) {
                is StorySource.Story -> {
                    api.uploadStoryPhotosFromUri(context, source.id, it) { photoUrls ->
                        addPart(StoryContent.Photos(photoUrls))
                        addPart(
                            StoryContent.Text("")
                        )
                    }
                }

                is StorySource.Card -> {
                    api.uploadCardContentPhotosFromUri(context, source.id, it) { photoUrls ->
                        addPart(StoryContent.Photos(photoUrls))
                        addPart(
                            StoryContent.Text("")
                        )
                    }
                }
            }
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it == null) return@rememberLauncherForActivityResult

        scope.launch {
            when (source) {
                is StorySource.Story -> {
                    api.uploadStoryAudioFromUri(context, source.id, it) { audioUrl ->
                        addPart(StoryContent.Audio(audioUrl))
                        addPart(
                            StoryContent.Text("")
                        )
                    }
                }

                is StorySource.Card -> {
                    api.uploadCardContentAudioFromUri(context, source.id, it) { audioUrl ->
                        addPart(StoryContent.Audio(audioUrl))
                        addPart(
                            StoryContent.Text("")
                        )
                    }
                }
            }
        }
    }

    if (showCardSelectorDialog) {
        ChooseCardDialog(
            {
                showCardSelectorDialog = false
            },
            navController = navController
        ) { cardId ->
            addPart(
                StoryContent.Cards(listOf(cardId))
            )
            addPart(
                StoryContent.Text("")
            )
        }
    }

    if (showCardGroupSelectorDialog) {
        val someone = stringResource(R.string.someone)
        val emptyGroup = stringResource(R.string.empty_group_name)

        ChooseGroupDialog(
            {
                showCardGroupSelectorDialog = false
            },
            title = stringResource(R.string.add_group),
            confirmFormatter = defaultConfirmFormatter(
                R.string.choose_group,
                R.string.choose_x,
                R.string.choose_x_and_x,
                R.string.choose_x_groups
            ) { it.name(someone, emptyGroup, me()?.id?.let(::listOf) ?: emptyList()) },
            infoFormatter = {
                buildString {
                    val count = it.members?.size ?: 0
                    append("$count ")
                    append(context.resources.getQuantityString(R.plurals.inline_members, count))
                    if (it.group?.description.isNullOrBlank().not()) {
                        append(" • ")
                        append(it.group!!.description)
                    }
                }
            },
            me = me(),
            filter = {
                it.group?.open == true
            }
        ) { groups ->
            addPart(
                StoryContent.Groups(groups.mapNotNull { it.id })
            )
        }
    }

    Card {
        var viewport by remember { mutableStateOf(Size(0f, 0f)) }
        val scrollState = rememberScrollState()
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
            modifier = Modifier
                .horizontalScroll(scrollState)
                .onPlaced { viewport = it.boundsInParent().size }
                .horizontalFadingEdge(viewport, scrollState)
        ) {
            listOf(
                Icons.Outlined.Title to {
                    addPart(
                        StoryContent.Section("")
                    )
                    addPart(
                        StoryContent.Text("")
                    )
                },
                Icons.Outlined.Notes to {
                    addPart(
                        StoryContent.Text("")
                    )
                },
                Icons.Outlined.Style to {
                    showCardSelectorDialog = true
                },
                Icons.Outlined.People to {
                    showCardGroupSelectorDialog = true
                },
                Icons.Outlined.Photo to {
                    photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                Icons.Outlined.PlayCircle to {
                    audioLauncher.launch("audio/*")
                }
            ).forEach {
                IconButton(
                    onClick = it.second
                ) {
                    Icon(it.first, null)
                }
            }
        }
    }
}
