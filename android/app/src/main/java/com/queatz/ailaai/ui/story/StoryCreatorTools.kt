package com.queatz.ailaai.ui.story

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.ailaai.api.createGroup
import app.ailaai.api.updateGroup
import com.queatz.ailaai.R
import com.queatz.ailaai.api.uploadCardContentAudioFromUri
import com.queatz.ailaai.api.uploadCardContentPhotosFromUri
import com.queatz.ailaai.api.uploadProfileContentAudioFromUri
import com.queatz.ailaai.api.uploadProfileContentPhotosFromUri
import com.queatz.ailaai.api.uploadStoryAudioFromUri
import com.queatz.ailaai.api.uploadStoryPhotosFromUri
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.LoadingIcon
import com.queatz.ailaai.ui.dialogs.ChooseCardDialog
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialogState
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.theme.pad
import com.queatz.ailaai.ui.widget.AddWidgetDialog
import com.queatz.db.Group
import com.queatz.db.StoryContent
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.ImpactEffortTableData
import com.queatz.widgets.widgets.SpaceData
import createWidget
import kotlinx.coroutines.launch

@Composable
fun StoryCreatorTools(
    source: StorySource,
    addPart: (part: StoryContent) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCardSelectorDialog by rememberStateOf(false)
    var showCardGroupSelectorDialog by rememberStateOf(false)
    var showAddProfilesDialog by rememberStateOf(false)
    var showCreateGroupDialog by rememberStateOf(false)
    var showWidgetsMenu by rememberStateOf(false)
    var showPhotoDialog by rememberStateOf(false)
    var isGeneratingPhoto by rememberStateOf(false)
    var isUploadingAudio by remember { mutableStateOf(false) }
    val photoState = remember {
        ChoosePhotoDialogState(mutableStateOf(""))
    }
    var addWidget by rememberStateOf<Widgets?>(null)

    addWidget?.let {
        AddWidgetDialog(
            onDismissRequest = {
                addWidget = null
            },
            source = source,
            widget = it
        ) { widget ->
            addPart(StoryContent.Widget(widget.widget!!, widget.id!!))
            addWidget = null
        }
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

                        else -> {}
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

                        else -> {}
                    }
                }
            },
            onIsGeneratingPhoto = {
                isGeneratingPhoto = it
            }
        )
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it == null) return@rememberLauncherForActivityResult

        isUploadingAudio = true

        scope.launch {
            when (source) {
                is StorySource.Story -> {
                    api.uploadStoryAudioFromUri(context, source.id, it) { audioUrl ->
                        addPart(StoryContent.Audio(audioUrl))
                        addPart(
                            StoryContent.Text("")
                        )
                        isUploadingAudio = false
                    }
                }

                is StorySource.Card -> {
                    api.uploadCardContentAudioFromUri(context, source.id, it) { audioUrl ->
                        addPart(StoryContent.Audio(audioUrl))
                        addPart(
                            StoryContent.Text("")
                        )
                        isUploadingAudio = false
                    }
                }

                is StorySource.Profile -> {
                    api.uploadProfileContentAudioFromUri(context, source.id, it) { audioUrl ->
                        addPart(StoryContent.Audio(audioUrl))
                        addPart(
                            StoryContent.Text("")
                        )
                        isUploadingAudio = false
                    }
                }

                else -> {
                    isUploadingAudio = false
                }
            }
        }
    }

    if (showWidgetsMenu) {
        Menu({
            showWidgetsMenu = false
        }) {
            // todo add search here
            menuItem(Widgets.Script.stringResource) {
                addWidget = Widgets.Script
                showWidgetsMenu = false
            }
            menuItem(Widgets.Space.stringResource) {
                scope.launch {
                    api.createWidget(
                        widget = Widgets.Space,
                        data = json.encodeToString(SpaceData(card = (source as? StorySource.Card)?.id))
                    ) {
                        addPart(StoryContent.Widget(it.widget!!, it.id!!))
                    }
                }
                showWidgetsMenu = false
            }
            menuItem(Widgets.Web.stringResource) {
                addWidget = Widgets.Web
                showWidgetsMenu = false
            }
            menuItem(Widgets.ImpactEffortTable.stringResource) {
                scope.launch {
                    api.createWidget(
                        widget = Widgets.ImpactEffortTable,
                        data = json.encodeToString(ImpactEffortTableData(card = (source as? StorySource.Card)?.id))
                    ) {
                        addPart(StoryContent.Widget(it.widget!!, it.id!!))
                    }
                }
                showWidgetsMenu = false
            }
            menuItem(Widgets.PageTree.stringResource) {
                addWidget = Widgets.PageTree
                showWidgetsMenu = false
            }
            menuItem(Widgets.Form.stringResource) {
                addWidget = Widgets.Form
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
            showCardSelectorDialog = false
        }
    }

    if (showCreateGroupDialog) {
        TextFieldDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = stringResource(R.string.group_name),
            button = stringResource(R.string.create_group),
            singleLine = true,
            placeholder = stringResource(R.string.empty_group_name),
            requireModification = false
        ) { value ->
            api.createGroup(emptyList()) { group ->
                if (value.isNotBlank()) {
                    api.updateGroup(group.id!!, Group(name = value))
                }
                addPart(
                    StoryContent.Groups(group.id!!.inList())
                )
            }
            showCreateGroupDialog = false
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
            actions = {
                IconButton(
                    {
                        showCreateGroupDialog = true
                        showCardGroupSelectorDialog = false
                    }
                ) {
                    Icon(Icons.Outlined.Add, null)
                }
            },
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
            }
        ) { groups ->
            addPart(
                StoryContent.Groups(groups.mapNotNull { it.id })
            )
        }
    }

    if (showAddProfilesDialog) {
        ChoosePeopleDialog(
            onDismissRequest = { showAddProfilesDialog = false },
            title = stringResource(R.string.people),
            confirmFormatter = { stringResource(R.string.add) },
            multiple = true,
            onPeopleSelected = {
                addPart(
                    StoryContent.Profiles(it.map { it.id!! }.distinct())
                )
                showAddProfilesDialog = false
            }
        )
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
            IconButton(
                onClick = {
                    addPart(
                        StoryContent.Section("")
                    )
                    addPart(
                        StoryContent.Text("")
                    )
                }
            ) {
                Icon(Icons.Outlined.Title, null)
            }
            IconButton(
                onClick = {
                    addPart(
                        StoryContent.Text("")
                    )
                }
            ) {
                Icon(Icons.Outlined.Notes, null)
            }
            IconButton(
                onClick = {
                    showCardSelectorDialog = true
                }
            ) {
                Icon(Icons.Outlined.Map, null)
            }
            IconButton(
                onClick = {
                    showAddProfilesDialog = true
                }
            ) {
                Icon(Icons.Outlined.PersonAdd, null)
            }
            IconButton(
                onClick = {
                    showCardGroupSelectorDialog = true
                }
            ) {
                Icon(Icons.Outlined.People, null)
            }
            IconButton(
                onClick = {
                    showPhotoDialog = true
                }
            ) {
                if (isGeneratingPhoto) {
                    LoadingIcon()
                } else {
                    Icon(Icons.Outlined.Photo, null)
                }
            }
            IconButton(
                onClick = {
                    audioLauncher.launch("audio/*")
                }
            ) {
                if (isUploadingAudio) {
                    LoadingIcon()
                } else {
                    Icon(Icons.Outlined.PlayCircle, null)
                }
            }
            IconButton(
                onClick = {
                    showWidgetsMenu = true
                }
            ) {
                Icon(Icons.Outlined.MoreHoriz, null)
            }
        }
    }
}
