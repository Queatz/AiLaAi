package com.queatz.ailaai.ui.story

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.card
import app.ailaai.api.group
import app.ailaai.api.updateCard
import coil.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.api.*
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.story.editor.ReorderStoryContentsDialog
import com.queatz.ailaai.ui.story.editor.SaveChangesDialog
import com.queatz.ailaai.ui.story.editor.StoryMenu
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.Card
import com.queatz.db.GroupExtended
import com.queatz.db.Story
import com.queatz.db.StoryDraft
import com.queatz.widgets.Widgets
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonArray

sealed class StorySource {
    data class Card(val id: String) : StorySource()
    data class Story(val id: String) : StorySource()
}

@Composable
fun StoryCreatorScreen(
    source: StorySource
) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val context = LocalContext.current
    var isLoading by rememberStateOf(true)
    var showPublishDialog by rememberStateOf(false)
    var showBackDialog by rememberStateOf(false)
    var showMenu by rememberStateOf(false)
    var showReorderContentDialog by rememberStateOf(false)
    var edited by rememberStateOf(false)
    var currentFocus by rememberStateOf(0)
    var story by rememberStateOf<Story?>(null)
    var card by rememberStateOf<Card?>(null)
    var storyContents by remember { mutableStateOf(emptyList<StoryContent>()) }
    val recompose = currentRecomposeScope
    val nav = nav
    val me = me

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
                    recompose.invalidate()
                }
            }
            is StorySource.Card -> {
                api.card(source.id) {
                    card = it
                    storyContents = card?.content?.asStoryContents() ?: emptyList()
                    recompose.invalidate()
                }
            }
        }
        isLoading = false
    }

    // todo use a loading/error/empty scaffold
    if (isLoading) {
        Loading()
        return
    }

    fun addPart(part: StoryContent, position: Int? = null) {
        val index = position ?: (currentFocus + 1).coerceAtMost(storyContents.size)
        storyContents = storyContents.toMutableList().apply {
            add(index, part)
        }
        edited = true
        recompose.invalidate()
        currentFocus = index
    }

    fun removePartAt(position: Int) {
        storyContents = storyContents.toMutableList().apply {
            removeAt(position)
        }
        edited = true
        recompose.invalidate()
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
                        content = json.encodeToString(buildJsonArray {
                            storyContents.filter { it.isPart() }.forEach { part ->
                                add(part.toJsonStoryPart())
                            }
                        })
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
                    context.toast(R.string.published)
                    nav.popBackStackOrFinish()
                }
            }
            else -> {}
        }
    }

    fun <T : StoryContent> T.edit(block: T.() -> Unit) {
        block()
        edited = true
        recompose.invalidate()
    }

    if (story == null && card == null) {
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
            recompose.invalidate()
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
            if (story != null) {
                StoryTitle(state, story)
            } else if (card != null) {
                Text(
                    stringResource(R.string.content),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = PaddingDefault),
                )
            }
            IconButton(
                {
                    showMenu = true
                }
            ) {
                Icon(
                    Icons.Outlined.MoreVert,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (source) {
                    is StorySource.Story -> {
                        StoryMenu(
                            showMenu,
                            {
                                showMenu = false
                            },
                            source.id,
                            story,
                            isMine = story?.person == me?.id,
                            edited = edited,
                            editing = true,
                            onReorder = {
                                showReorderContentDialog = true
                            }
                        )
                    }

                    is StorySource.Card -> {
                        Dropdown(
                            showMenu,
                            {
                                showMenu = false
                            }
                        ) {
                            DropdownMenuItem({ Text(stringResource(R.string.reorder)) }, {
                                showMenu = false
                                showReorderContentDialog = true
                            })
                        }
                    }
                }
            }

            if (edited) {
                Button(
                    onClick = {
                        scope.launch {
                            save()
                        }
                    },
                    modifier = Modifier.padding(end = PaddingDefault * 2)
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
                            modifier = Modifier.padding(end = PaddingDefault * 2)
                        ) {
                            Text(stringResource(R.string.publish))
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
            contentPadding = PaddingValues(PaddingDefault * 2),
            horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
            verticalArrangement = Arrangement.spacedBy(PaddingDefault),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(bottom = PaddingDefault)
                .weight(1f)
        ) {
            storyContents.forEachIndexed { partIndex, part ->
                when (part) {
                    is StoryContent.Title -> {
                        item(span = { GridItemSpan(maxLineSpan) }, key = part.key) {
                            val focusRequester = remember { FocusRequester() }
                            LaunchedEffect(currentFocus) {
                                if (currentFocus == partIndex) {
                                    focusRequester.requestFocus()
                                }
                            }
                            EditorTextField(
                                part.title,
                                {
                                    part.edit {
                                        title = it
                                    }
                                },
                                focusRequester = focusRequester,
                                placeholder = stringResource(R.string.title),
                                singleLine = true,
                                onFocus = {
                                    currentFocus = partIndex
                                },
                                onNext = { addPart(position = partIndex + 1, part = StoryContent.Text("")) },
                                textStyle = { headlineMedium }
                            )
                        }
                    }

                    is StoryContent.Section -> {
                        item(span = { GridItemSpan(maxLineSpan) }, key = part.key) {
                            val focusRequester = remember { FocusRequester() }
                            LaunchedEffect(currentFocus) {
                                if (currentFocus == partIndex) {
                                    focusRequester.requestFocus()
                                }
                            }
                            EditorTextField(
                                part.section,
                                {
                                    part.edit {
                                        section = it
                                    }
                                },
                                focusRequester = focusRequester,
                                placeholder = stringResource(R.string.section),
                                singleLine = true,
                                onDelete = {
                                    removePartAt(partIndex)
                                },
                                onFocus = {
                                    currentFocus = partIndex
                                },
                                onNext = { addPart(position = partIndex + 1, part = StoryContent.Text("")) },
                                textStyle = { titleLarge }
                            )
                        }
                    }

                    is StoryContent.Text -> {
                        item(span = { GridItemSpan(maxLineSpan) }, key = part.key) {
                            val focusRequester = remember { FocusRequester() }
                            LaunchedEffect(currentFocus) {
                                if (currentFocus == partIndex) {
                                    focusRequester.requestFocus()
                                }
                            }
                            EditorTextField(
                                part.text,
                                {
                                    part.edit {
                                        text = it
                                    }
                                },
                                focusRequester = focusRequester,
                                placeholder = stringResource(R.string.write),
                                onDelete = {
                                    removePartAt(partIndex)
                                },
                                onFocus = {
                                    currentFocus = partIndex
                                },
                                textStyle = { bodyMedium }
                            )
                        }
                    }

                    is StoryContent.Audio -> {
                        item(span = { GridItemSpan(maxLineSpan) }, key = part.key) {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.large)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault)
                                ) {
                                    Audio(
                                        api.url(part.audio),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = {
                                            removePartAt(partIndex)
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    is StoryContent.Groups -> {
                        itemsIndexed(
                            part.groups,
                            span = { _, _ -> GridItemSpan(maxLineSpan) },
                            key = { index, it -> "${part.key}.$it" }
                        ) { index, groupId ->
                            var group by remember { mutableStateOf<GroupExtended?>(null) }
                            var showGroupMenu by rememberStateOf(false)
                            var showAddGroupDialog by rememberStateOf(false)
                            var showReorderDialog by rememberStateOf(false)

                            if (showAddGroupDialog) {
                                val someone = stringResource(R.string.someone)
                                val emptyGroup = stringResource(R.string.empty_group_name)

                                ChooseGroupDialog(
                                    {
                                        showAddGroupDialog = false
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
                                        it.group?.open == true && part.groups.none { id -> it.group?.id == id }
                                    }
                                ) {
                                    part.edit {
                                        groups += it.mapNotNull { it.id }
                                    }
                                }
                            }

                            if (showReorderDialog) {
                                ReorderDialog(
                                    { showReorderDialog = false },
                                    onMove = { from, to ->
                                        part.edit {
                                            groups = groups.toMutableList().apply {
                                                add(to.index, removeAt(from.index))
                                            }
                                        }
                                    },
                                    list = true,
                                    items = part.groups,
                                    key = { it }
                                ) { groupId, elevation ->
                                    var group by remember { mutableStateOf<GroupExtended?>(null) }

                                    LaunchedEffect(groupId) {
                                        api.group(groupId) { group = it }
                                    }

                                    LoadingText(group != null, stringResource(R.string.loading_group)) {
                                        ContactItem(
                                            onClick = null,
                                            onLongClick = null,
                                            item = SearchResult.Group(group!!),
                                            info = GroupInfo.LatestMessage
                                        )
                                    }
                                }
                            }

                            if (showGroupMenu) {
                                Menu(
                                    {
                                        showGroupMenu = false
                                    }
                                ) {
                                    menuItem(stringResource(R.string.add_group)) {
                                        showGroupMenu = false
                                        showAddGroupDialog = true
                                    }
                                    menuItem(stringResource(R.string.open_group)) {
                                        showGroupMenu = false
                                        nav.navigate("group/$groupId")
                                    }
                                    if (part.groups.size > 1) {
                                        menuItem(stringResource(R.string.reorder)) {
                                            showGroupMenu = false
                                            showReorderDialog = true
                                        }
                                    }
                                    menuItem(stringResource(R.string.remove)) {
                                        showGroupMenu = false
                                        if (part.groups.size == 1) {
                                            showGroupMenu = false
                                            removePartAt(partIndex)
                                        } else {
                                            part.edit {
                                                groups = groups.toMutableList().apply {
                                                    removeAt(index)
                                                }
                                            }
                                        }
                                    }
                                    if (part.groups.size > 1) {
                                        menuItem(stringResource(R.string.remove_all)) {
                                            showGroupMenu = false
                                            removePartAt(partIndex)
                                        }
                                    }
                                }
                            }

                            LaunchedEffect(groupId) {
                                api.group(groupId) { group = it }
                            }

                            LoadingText(group != null, stringResource(R.string.loading_group)) {
                                ContactItem(
                                    onClick = {
                                        showGroupMenu = true
                                    },
                                    onLongClick = {},
                                    SearchResult.Group(group!!),
                                    info = GroupInfo.LatestMessage
                                )
                            }
                        }
                    }

                    is StoryContent.Cards -> {
                        itemsIndexed(part.cards, key = { index, it -> "${part.key}.$it" }) { index, cardId ->
                            var showCardMenu by rememberStateOf(false)
                            var showAddCardDialog by rememberStateOf(false)
                            var showReorderDialog by rememberStateOf(false)
                            var card by remember { mutableStateOf<Card?>(null) }

                            if (showAddCardDialog) {
                                ChooseCardDialog(
                                    {
                                        showAddCardDialog = false
                                    },
                                ) {
                                    part.edit {
                                        cards = cards + it
                                    }
                                }
                            }

                            if (showReorderDialog) {
                                ReorderDialog(
                                    { showReorderDialog = false },
                                    onMove = { from, to ->
                                        part.edit {
                                            cards = cards.toMutableList().apply {
                                                add(to.index, removeAt(from.index))
                                            }
                                        }
                                    },
                                    items = part.cards,
                                    key = { it }
                                ) { cardId, elevation ->
                                    var card by remember { mutableStateOf<Card?>(null) }

                                    LaunchedEffect(cardId) {
                                        api.card(cardId) { card = it }
                                    }

                                    CardItem(
                                        onClick = null,
                                        card = card,
                                        isChoosing = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            LaunchedEffect(cardId) {
                                api.card(cardId) { card = it }
                            }

                            if (showCardMenu) {
                                Menu(
                                    {
                                        showCardMenu = false
                                    }
                                ) {
                                    menuItem(stringResource(R.string.add_card)) {
                                        showCardMenu = false
                                        showAddCardDialog = true
                                    }
                                    menuItem(stringResource(R.string.open_card)) {
                                        showCardMenu = false
                                        nav.navigate("card/$cardId")
                                    }
                                    if (part.cards.size > 1) {
                                        menuItem(stringResource(R.string.reorder)) {
                                            showCardMenu = false
                                            showReorderDialog = true
                                        }
                                    }
                                    menuItem(stringResource(R.string.remove)) {
                                        showCardMenu = false
                                        if (part.cards.size == 1) {
                                            showCardMenu = false
                                            removePartAt(partIndex)
                                        } else {
                                            part.edit {
                                                cards = cards.toMutableList().apply {
                                                    removeAt(index)
                                                }
                                            }
                                        }
                                    }
                                    if (part.cards.size > 1) {
                                        menuItem(stringResource(R.string.remove_all)) {
                                            showCardMenu = false
                                            removePartAt(partIndex)
                                        }
                                    }
                                }
                            }

                            CardItem(
                                {
                                    showCardMenu = true
                                },
                                onCategoryClick = {},
                                card = card,
                                isChoosing = true,
                                modifier = Modifier.fillMaxWidth(.75f)
                            )
                        }
                    }

                    is StoryContent.Photos -> {
                        itemsIndexed(
                            part.photos,
                            span = { index, item ->
                                GridItemSpan(if (index == 0) maxLineSpan else if (index % 3 == 1) 1 else maxCurrentLineSpan)
                            },
                            key = { index, it -> "${part.key}.$it" }
                        ) { index, it ->
                            var showPhotoMenu by rememberStateOf(false)
                            var showPhotoAspectMenu by rememberStateOf(false)
                            var showReorderDialog by rememberStateOf(false)

                            val photoLauncher =
                                rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) {
                                    if (it.isEmpty()) return@rememberLauncherForActivityResult

                                    scope.launch {
                                        when (source) {
                                            is StorySource.Story -> {
                                                api.uploadStoryPhotosFromUri(context, source.id, it) { photoUrls ->
                                                    part.edit {
                                                        photos += photoUrls
                                                    }
                                                }
                                            }

                                            is StorySource.Card -> {
                                                api.uploadCardContentPhotosFromUri(
                                                    context,
                                                    source.id,
                                                    it
                                                ) { photoUrls ->
                                                    part.edit {
                                                        photos += photoUrls
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            if (showPhotoAspectMenu) {
                                Menu(
                                    {
                                        showPhotoAspectMenu = false
                                    }
                                ) {
                                    menuItem(stringResource(R.string.portrait)) {
                                        showPhotoAspectMenu = false
                                        part.edit {
                                            aspect = .75f
                                        }
                                    }
                                    menuItem(stringResource(R.string.landscape)) {
                                        showPhotoAspectMenu = false
                                        part.edit {
                                            aspect = 1.5f
                                        }
                                    }
                                    menuItem(stringResource(R.string.square)) {
                                        showPhotoAspectMenu = false
                                        part.edit {
                                            aspect = 1f
                                        }
                                    }
                                }
                            }

                            if (showReorderDialog) {
                                ReorderDialog(
                                    { showReorderDialog = false },
                                    onMove = { from, to ->
                                        part.edit {
                                            photos = photos.toMutableList().apply {
                                                add(to.index, removeAt(from.index))
                                            }
                                        }
                                    },
                                    items = part.photos,
                                    key = { it }
                                ) { photo, elevation ->
                                    AsyncImage(
                                        model = api.url(photo),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.Center,
                                        modifier = Modifier
                                            .shadow(elevation, shape = MaterialTheme.shapes.large)
                                            .clip(MaterialTheme.shapes.large)
                                            .aspectRatio(part.aspect)
                                            .heightIn(min = 240.dp)
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                    )
                                }
                            }

                            if (showPhotoMenu) {
                                Menu(
                                    {
                                        showPhotoMenu = false
                                    }
                                ) {
                                    menuItem(stringResource(R.string.add_photo)) {
                                        showPhotoMenu = false
                                        photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                    menuItem(stringResource(R.string.change_aspect_ratio)) {
                                        showPhotoMenu = false
                                        showPhotoAspectMenu = true
                                    }
                                    if (part.photos.size > 1) {
                                        menuItem(stringResource(R.string.reorder)) {
                                            showReorderDialog = true
                                            showPhotoMenu = false
                                        }
                                    }
                                    menuItem(stringResource(R.string.remove)) {
                                        showPhotoMenu = false
                                        if (part.photos.size == 1) {
                                            showPhotoMenu = false
                                            removePartAt(partIndex)
                                        } else {
                                            part.edit {
                                                photos = photos.toMutableList().apply {
                                                    removeAt(index)
                                                }
                                            }
                                        }
                                    }
                                    if (part.photos.size > 1) {
                                        menuItem(stringResource(R.string.remove_all)) {
                                            showPhotoMenu = false
                                            removePartAt(partIndex)
                                        }
                                    }
                                }
                            }

                            AsyncImage(
                                model = api.url(it),
                                contentDescription = "",
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.large)
                                    .fillMaxWidth()
                                    .aspectRatio(part.aspect)
                                    .heightIn(min = 240.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable {
                                        showPhotoMenu = true
                                    }
                            )
                        }
                    }

                    is StoryContent.Widget -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            var showWidgetMenu by rememberStateOf(false)

                            if (showWidgetMenu) {
                                Menu({
                                    showWidgetMenu = false
                                }) {
                                    menuItem(stringResource(R.string.remove)) {
                                        showWidgetMenu = false
                                        removePartAt(partIndex)
                                    }
                                }
                            }

                            Stub(part.widget.stringResource) {
                                showWidgetMenu = true
                            }
                        }
                    }

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

val Widgets.stringResource @Composable get() = stringResource(
    when (this) {
        Widgets.ImpactEffortTable -> R.string.impact_effort_table
        else -> R.string.widget
    }
)
