package com.queatz.ailaai.ui.story

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.card
import app.ailaai.api.profile
import app.ailaai.api.updateCard
import app.ailaai.api.updateProfile
import com.queatz.ailaai.R
import com.queatz.ailaai.api.story
import com.queatz.ailaai.api.updateStory
import com.queatz.ailaai.api.updateStoryDraft
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.popBackStackOrFinish
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toList
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.LoadingIcon
import com.queatz.ailaai.ui.story.creator.audioCreatorItem
import com.queatz.ailaai.ui.story.creator.buttonCreatorItem
import com.queatz.ailaai.ui.story.creator.cardsCreatorItem
import com.queatz.ailaai.ui.story.creator.groupsCreatorItem
import com.queatz.ailaai.ui.story.creator.inputCreatorItem
import com.queatz.ailaai.ui.story.creator.photosCreatorItem
import com.queatz.ailaai.ui.story.creator.profilesCreatorItem
import com.queatz.ailaai.ui.story.creator.sectionCreatorItem
import com.queatz.ailaai.ui.story.creator.textCreatorItem
import com.queatz.ailaai.ui.story.creator.titleCreatorItem
import com.queatz.ailaai.ui.story.creator.widgetCreatorItem
import com.queatz.ailaai.ui.story.editor.ReorderStoryContentsDialog
import com.queatz.ailaai.ui.story.editor.SaveChangesDialog
import com.queatz.ailaai.ui.story.editor.StoryMenu
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.PersonProfile
import com.queatz.db.Profile
import com.queatz.db.Story
import com.queatz.db.StoryContent
import com.queatz.db.StoryDraft
import com.queatz.db.isPart
import com.queatz.db.toJsonStoryContent
import com.queatz.widgets.Widgets
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonArray

sealed class StorySource {
    data class Card(val id: String) : StorySource()
    data class Story(val id: String) : StorySource()
    data class Profile(val id: String) : StorySource()
    data class Script(val id: String) : StorySource()
}

@Composable
fun StoryCreatorScreen(
    source: StorySource,
) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val context = LocalContext.current
    var isLoading by rememberStateOf(true)
    var isLoadingMenu by rememberStateOf(false)
    var showPublishDialog by rememberStateOf(false)
    var showBackDialog by rememberStateOf(false)
    var showMenu by rememberStateOf(false)
    var showReorderContentDialog by rememberStateOf(false)
    var edited by rememberStateOf(false)
    var currentFocus by rememberStateOf(0)
    var story by rememberStateOf<Story?>(null)
    var card by rememberStateOf<Card?>(null)
    var profile by rememberStateOf<PersonProfile?>(null)
    var storyContents by remember { mutableStateOf(emptyList<StoryContent>()) }
    val recompose = currentRecomposeScope
    val nav = nav
    val me = me

    // Kotlin 2.0 upgrade stopped recompose.invalidate() from working
    fun invalidate() {
        val saved = storyContents
        storyContents = emptyList()
        recompose.invalidate()
        storyContents = saved
    }

    BackHandler(enabled = edited && !showBackDialog) {
        showBackDialog = true
    }

    LaunchedEffect(Unit) {
        isLoading = true
        when (source) {
            is StorySource.Story -> {
                api.story(source.id) {
                    story = it
                    storyContents = listOf(
                        StoryContent.Title(story?.title ?: "", source.id)
                    ) + story!!.contents()
                    invalidate()
                }
            }

            is StorySource.Card -> {
                api.card(source.id) {
                    card = it
                    storyContents = card?.content?.asStoryContents() ?: emptyList()
                    invalidate()
                }
            }

            is StorySource.Profile -> {
                api.profile(source.id) {
                    profile = it
                    storyContents = profile?.profile?.content?.asStoryContents() ?: emptyList()
                    invalidate()
                }
            }

            else -> {}
        }
        isLoading = false
    }

    // todo use a loading/error/empty scaffold
    if (isLoading) {
        Column(modifier = Modifier.fillMaxSize()) {
            Loading()
        }
        return
    }

    fun addPart(part: StoryContent, position: Int? = null) {
        val index = position ?: (currentFocus + 1).coerceAtMost(storyContents.size)
        storyContents = storyContents.toMutableList().apply {
            add(index, part)
        }
        edited = true
        invalidate()
        currentFocus = index
    }

    fun removePartAt(position: Int) {
        storyContents = storyContents.toMutableList().apply {
            removeAt(position)
        }
        edited = true
        invalidate()
        currentFocus = (position - 1).coerceAtLeast(0)
    }

    suspend fun save(): Boolean {
        var hasError = false

        when (source) {
            is StorySource.Story -> {
                api.updateStory(
                    source.id,
                    Story(
                        // todo only send title if it was edited
                        title = storyContents.firstNotNullOfOrNull { it as? StoryContent.Title }?.title,
                        content = storyContents.toJsonStoryContent(json)
                    ),
                    onError = {
                        hasError = true
                    }
                ) {
                    story = it
                    edited = false
                }
            }

            is StorySource.Card -> {
                api.updateCard(
                    source.id,
                    Card(
                        content = json.encodeToString(buildJsonArray {
                            storyContents.filter { it.isPart() }.forEach { part ->
                                add(part.toJsonStoryPart())
                            }
                        })
                    )
                ) {
                    card = it
                    edited = false
                }
            }

            is StorySource.Profile -> {
                api.updateProfile(Profile(content = json.encodeToString(buildJsonArray {
                    storyContents.filter { it.isPart() }.forEach { part ->
                        add(part.toJsonStoryPart())
                    }
                }))) {
                    profile = profile?.copy(profile = it)
                    edited = false
                }
            }

            else -> {}
        }

        return !hasError
    }

    fun isBlankText(storyContent: StoryContent) = when (storyContent) {
        is StoryContent.Section -> storyContent.section.isBlank()
        is StoryContent.Text -> storyContent.text.isBlank()
        else -> false
    }

    suspend fun publish() {
        if (edited) return context.showDidntWork()

        if (storyContents.any(::isBlankText)) {
            storyContents = storyContents.toMutableList().filterNot(::isBlankText)
            save()
        }
        when (source) {
            is StorySource.Story -> {
                api.updateStory(source.id, Story(published = true)) {
                    context.toast(R.string.posted)
                    nav.popBackStackOrFinish()
                }
            }

            else -> {}
        }
    }

    fun <T : StoryContent> T.edit(block: T.() -> Unit) {
        block()
        edited = true
        invalidate()
    }

    // todo make sealed class
    if (story == null && card == null && profile == null) {
        return
    }

    if (showPublishDialog) {
        val storyId = (source as StorySource.Story).id
        PublishStoryDialog(
            {
                showPublishDialog = false
            },
            nav.context as Activity,
            story!!,
            storyContents,
            onLocationChanged = {
                scope.launch {
                    api.updateStory(storyId, Story(geo = it?.toList() ?: emptyList())) {
                        story?.geo = it.geo
                    }
                }
            },
            onGroupsChanged = { groups ->
                scope.launch {
                    api.updateStoryDraft(storyId, StoryDraft(groups = groups.map { it.id!! })) {}
                }
            }
        ) {
            scope.launch {
                publish()
            }
        }
    }

    if (showBackDialog) {
        SaveChangesDialog(
            {
                showBackDialog = false
            },
            onDiscard = {
                nav.popBackStack()
            },
            onSave = {
                scope.launch {
                    if (save()) {
                        nav.popBackStack()
                    }
                }
            }
        )
    }

    if (showReorderContentDialog) {
        ReorderStoryContentsDialog(
            {
                showReorderContentDialog = false
            },
            storyContents
        ) {
            storyContents = it
            edited = true
            invalidate()
        }
    }

    StoryScaffold(
        {
            if (edited) {
                showBackDialog = true
            } else {
                nav.popBackStack()
            }
        },
        actions = {
            val title = storyContents.firstNotNullOfOrNull { it as? StoryContent.Title }?.title
            if (story != null) {
                StoryTitle(
                    state,
                    title ?: story?.title
                )
            } else if (card != null) {
                Text(
                    card?.name ?: stringResource(R.string.content),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.pad),
                )
            } else if (profile != null) {
                Text(
                    stringResource(R.string.profile),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.pad),
                )
            }
            IconButton(
                {
                    showMenu = true
                }
            ) {
                if (isLoadingMenu) {
                    LoadingIcon()
                } else {
                    Icon(
                        Icons.Outlined.MoreVert,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (source) {
                    is StorySource.Story -> {
                        StoryMenu(
                            expanded = showMenu,
                            onDismissRequest = {
                                showMenu = false
                            },
                            storyId = source.id,
                            story = story,
                            isMine = story?.person == me?.id,
                            edited = edited,
                            editing = true,
                            onIsLoading = {
                                isLoadingMenu = it
                            },
                            onReorder = {
                                showReorderContentDialog = true
                            }
                        )
                    }

                    is StorySource.Card,
                    is StorySource.Profile,
                        -> {
                        Dropdown(
                            expanded = showMenu,
                            onDismissRequest = {
                                showMenu = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reorder)) },
                                onClick = {
                                    showMenu = false
                                    showReorderContentDialog = true
                                }
                            )
                        }
                    }

                    else -> {}
                }
            }

            if (edited) {
                Button(
                    onClick = {
                        scope.launch {
                            save()
                        }
                    },
                    modifier = Modifier.padding(end = 2.pad)
                ) {
                    Text(stringResource(R.string.save))
                }
            } else {
                when (source) {
                    is StorySource.Story -> {
                        OutlinedButton(
                            onClick = {
                                showPublishDialog = true
                            },
                            modifier = Modifier.padding(end = 2.pad)
                        ) {
                            Text(stringResource(R.string.post))
                        }
                    }

                    else -> {}
                }
            }
        }
    ) {
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(2.pad),
            horizontalArrangement = Arrangement.spacedBy(1.pad),
            verticalArrangement = Arrangement.spacedBy(1.pad),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(bottom = 1.pad)
                .weight(1f)
        ) {
            fun <T : StoryContent> creatorScope(part: T, partIndex: Int) = CreatorScope(
                source = source,
                part = part,
                partIndex = partIndex,
                currentFocus = currentFocus,
                onCurrentFocus = { currentFocus = it },
                add = ::addPart,
                remove = ::removePartAt,
                edit = part::edit
            )

            storyContents.forEachIndexed { partIndex, part ->
                when (part) {
                    is StoryContent.Title -> titleCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Section -> sectionCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Text -> textCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Audio -> audioCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Groups -> groupsCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Cards -> cardsCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Photos -> photosCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Widget -> widgetCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Button -> buttonCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Input -> inputCreatorItem(creatorScope(part, partIndex))

                    is StoryContent.Profiles -> profilesCreatorItem(creatorScope(part, partIndex))

                    else -> {
                        // Not supported in the editor
                    }
                }
            }
        }
        StoryCreatorTools(
            source,
            ::addPart
        )
    }
}

val Widgets.stringResource
    @Composable get() = stringResource(
        when (this) {
            Widgets.ImpactEffortTable -> R.string.impact_effort_table
            Widgets.PageTree -> R.string.page_tree
            Widgets.Script -> R.string.script
            Widgets.Web -> R.string.website
            Widgets.Form -> R.string.form
            Widgets.Space -> R.string.space
            Widgets.Shop -> R.string.shop
        }
    )
