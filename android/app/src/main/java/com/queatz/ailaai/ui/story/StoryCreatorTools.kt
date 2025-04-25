package com.queatz.ailaai.ui.story

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FormatShapes
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TableChart
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
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.Toolbar
import com.queatz.ailaai.ui.dialogs.ChooseCardDialog
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialogState
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.widget.AddWidgetDialog
import com.queatz.db.Group
import com.queatz.db.StoryContent
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.ImpactEffortTableData
import com.queatz.widgets.widgets.ShopData
import com.queatz.widgets.widgets.SpaceData
import createWidget
import kotlinx.coroutines.launch

@Composable
fun StoryCreatorTools(
    source: StorySource,
    addPart: (part: StoryContent) -> Unit
) {
    val context = LocalContext.current
    val me = me
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

                        is StorySource.Reminder -> {
                            api.uploadProfileContentPhotosFromUri(context, me!!.id!!, photoUrls) { photoUrls ->
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

                is StorySource.Reminder -> {
                    // todo: upload reminder audio
                    api.uploadProfileContentAudioFromUri(context, me!!.id!!, it) { audioUrl ->
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
            Toolbar {
                item(
                    icon = Icons.Outlined.Storefront,
                    name = Widgets.Shop.stringResource
                ) {
                    scope.launch {
                        api.createWidget(
                            widget = Widgets.Shop,
                            data = json.encodeToString(ShopData())
                        ) {
                            addPart(StoryContent.Widget(it.widget!!, it.id!!))
                        }
                    }
                    showWidgetsMenu = false
                }

                item(
                    icon = Icons.Outlined.HistoryEdu,
                    name = Widgets.Script.stringResource
                ) {
                    addWidget = Widgets.Script
                    showWidgetsMenu = false
                }

                item(
                    icon = Icons.Outlined.FormatShapes,
                    name = Widgets.Space.stringResource
                ) {
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

                item(
                    icon = Icons.Outlined.Language,
                    name = Widgets.Web.stringResource
                ) {
                    addWidget = Widgets.Web
                    showWidgetsMenu = false
                }

                item(
                    icon = Icons.Outlined.TableChart,
                    name = Widgets.ImpactEffortTable.stringResource
                ) {
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

                item(
                    icon = Icons.Outlined.AccountTree,
                    name = Widgets.PageTree.stringResource
                ) {
                addWidget = Widgets.PageTree
                showWidgetsMenu = false
                }

                item(
                    icon = Icons.Outlined.ListAlt,
                    name = Widgets.Form.stringResource
                ) {
                addWidget = Widgets.Form
                showWidgetsMenu = false
                }
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
        Toolbar(singleLine = true) {
            item(
                icon = Icons.Outlined.Title,
                name = stringResource(R.string.section),
            ) {
                addPart(
                    StoryContent.Section("")
                )
                addPart(
                    StoryContent.Text("")
                )
            }
            item(
                icon = Icons.AutoMirrored.Outlined.Notes,
                name = stringResource(R.string.text),
            ) {
                addPart(
                    StoryContent.Text("")
                )
            }
            item(
                icon = Icons.Outlined.Map,
                name = stringResource(R.string.card),
            ) {
                showCardSelectorDialog = true
            }
            item(
                icon = Icons.Outlined.PersonAdd,
                name = stringResource(R.string.profile),
            ) {
                showAddProfilesDialog = true
            }
            item(
                icon = Icons.Outlined.People,
                name = stringResource(R.string.group),
            ) {
                showCardGroupSelectorDialog = true
            }
            item(
                icon = Icons.Outlined.Photo,
                name = stringResource(R.string.photo),
                isLoading = isGeneratingPhoto
            ) {
                showPhotoDialog = true
            }
            item(
                icon = Icons.Outlined.PlayCircle,
                name = stringResource(R.string.audio),
                isLoading = isUploadingAudio
            ) {
                audioLauncher.launch("audio/*")
            }
            item(
                icon = Icons.Outlined.MoreHoriz,
                name = stringResource(R.string.more),
            ) {
                showWidgetsMenu = true
            }
        }
    }
}
