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
import com.queatz.ailaai.R
import com.queatz.ailaai.api.*
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.theme.pad
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.ImpactEffortTableData
import createWidget
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

@Composable
fun StoryCreatorTools(
    source: StorySource,
    addPart: (part: StoryContent) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCardSelectorDialog by rememberStateOf(false)
    var showCardGroupSelectorDialog by rememberStateOf(false)
    var showWidgetsMenu by rememberStateOf(false)
    var showPhotoDialog by rememberStateOf(false)
    val photoState = remember {
        ChoosePhotoDialogState(mutableStateOf(""))
    }

    if (showPhotoDialog) {
        ChoosePhotoDialog(
            scope = scope,
            onDismissRequest = { showPhotoDialog = false },
            imagesOnly = true,
            state = photoState,
            onPhotos = { photoUrls ->
                scope.launch {
                    when (source) {
                        is StorySource.Story -> {
                            api.uploadStoryPhotosFromUri(context, source.id, photoUrls) { photoUrls ->
                                addPart(StoryContent.Photos(photoUrls))
                                addPart(
                                    StoryContent.Text("")
                                )
                            }
                        }

                        is StorySource.Card -> {
                            api.uploadCardContentPhotosFromUri(context, source.id, photoUrls) { photoUrls ->
                                addPart(StoryContent.Photos(photoUrls))
                                addPart(
                                    StoryContent.Text("")
                                )
                            }
                        }

                        is StorySource.Profile -> {
                            api.uploadProfileContentPhotosFromUri(context, source.id, photoUrls) { photoUrls ->
                                addPart(StoryContent.Photos(photoUrls))
                                addPart(
                                    StoryContent.Text("")
                                )
                            }
                        }
                    }
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    when (source) {
                        is StorySource.Story -> {
                            addPart(StoryContent.Photos(listOf(photo)))
                            addPart(
                                StoryContent.Text("")
                            )
                        }

                        is StorySource.Card -> {
                            addPart(StoryContent.Photos(listOf(photo)))
                            addPart(
                                StoryContent.Text("")
                            )
                        }

                        is StorySource.Profile -> {
                            addPart(StoryContent.Photos(listOf(photo)))
                            addPart(
                                StoryContent.Text("")
                            )
                        }
                    }
                }
            }
        )
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

                is StorySource.Profile -> {
                    api.uploadProfileContentAudioFromUri(context, source.id, it) { audioUrl ->
                        addPart(StoryContent.Audio(audioUrl))
                        addPart(
                            StoryContent.Text("")
                        )
                    }
                }
            }
        }
    }

    if (showWidgetsMenu) {
        Menu({
            showWidgetsMenu = false
        }) {
            menuItem(stringResource(R.string.impact_effort_table)) {
                scope.launch {
                    api.createWidget(
                        Widgets.ImpactEffortTable,
                        data = json.encodeToString(ImpactEffortTableData(card = (source as? StorySource.Card)?.id))
                    ) {
                        addPart(StoryContent.Widget(it.widget!!, it.id!!))
                    }
                }
                showWidgetsMenu = false
            }
        }
    }

    if (showCardSelectorDialog) {
        ChooseCardDialog(
            {
                showCardSelectorDialog = false
            }
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
        val me = me

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
            ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) },
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
            horizontalArrangement = Arrangement.spacedBy(1.pad),
            modifier = Modifier
                .horizontalScroll(scrollState)
                .onPlaced { viewport = it.boundsInParent().size }
                .horizontalFadingEdge(viewport, scrollState)
        ) {
            listOfNotNull(
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
                    showPhotoDialog = true
                },
                Icons.Outlined.PlayCircle to {
                    audioLauncher.launch("audio/*")
                },
                Icons.Outlined.MoreHoriz to {
                    showWidgetsMenu = true
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
