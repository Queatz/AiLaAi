package com.queatz.ailaai.ui.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import app.ailaai.api.people
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.extensions.toList
import com.queatz.ailaai.helpers.LocationSelector
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.mePresence
import com.queatz.ailaai.ui.components.CardList
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.components.SearchFilter
import com.queatz.ailaai.ui.control.MapCardsControl
import com.queatz.ailaai.ui.event.EventsScreen
import com.queatz.ailaai.ui.screens.GroupsScreen
import com.queatz.ailaai.ui.screens.SearchContent
import com.queatz.ailaai.ui.sheet.SheetHeader
import com.queatz.ailaai.ui.story.editor.StoryActions
import com.queatz.ailaai.ui.components.PersonItem
import com.queatz.ailaai.ui.profile.ProfileCard
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import com.queatz.db.PersonProfile
import com.queatz.db.Story
import com.queatz.db.StoryContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var cache = emptyList<Story>()
private var peopleCache = emptyList<PersonProfile>()

class SheetScreenState {

    internal var state: LazyGridState? = null
    internal var cardsState: LazyGridState? = null
    internal var groupsState: LazyListState? = null
    internal var peopleState: LazyGridState? = null

    suspend fun scrollToTop() {
        state?.scrollToTop()
        cardsState?.scrollToTop()
        groupsState?.scrollToTop()
        peopleState?.scrollToTop()
    }
}

enum class SheetContent {
    Events,
    Posts,
    Groups,
    Pages,
    People
}

@Composable
fun SheetScreen(
    mapCardsControl: MapCardsControl,
    geo: LatLng?,
    myGeo: LatLng?,
    title: String? = null,
    hint: String? = null,
    distance: String? = null,
    onTitleClick: (() -> Unit)? = null,
    onExpandRequest: () -> Unit = {},
    sheetState: SheetScreenState = remember { SheetScreenState() },
    value: String,
    locationSelector: LocationSelector,
    filters: List<SearchFilter>,
    valueChange: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val cardsState = rememberLazyGridState()
    val groupsState = rememberLazyListState()
    val peopleState = rememberLazyGridState()
    val context = LocalContext.current
    var stories by remember { mutableStateOf(cache) }
    val storyContents = remember(stories) {
        stories.flatMapIndexed { index, story ->
            (if (index > 0) listOf(StoryContent.Divider) else emptyList()) +
                    story.asContents() +
                    listOf(
                        StoryContent.Reactions(story.id!!, story.reactions),
                        StoryContent.Comments(story.id!!)
                    )
        }
    }
    var isLoading by rememberStateOf(stories.isEmpty())
    var people by remember { mutableStateOf(peopleCache) }
    var isPeopleLoading by rememberStateOf(people.isEmpty())
    var peopleOffset by rememberStateOf(0)
    var hasMorePeople by rememberStateOf(true)
    var peopleSearchText by rememberSaveable { mutableStateOf("") }
    val nav = nav
    var showShareAThought by rememberStateOf(true)
    var sheetContent by rememberSavableStateOf(SheetContent.Posts)

    LaunchedEffect(state) {
        sheetState.state = state
    }

    LaunchedEffect(cardsState) {
        sheetState.cardsState = cardsState
    }

    LaunchedEffect(groupsState) {
        sheetState.groupsState = groupsState
    }

    LaunchedEffect(peopleState) {
        sheetState.peopleState = peopleState
    }

    LaunchedEffect(stories) {
        cache = stories
    }

    LaunchedEffect(people) {
        peopleCache = people
    }

    LaunchedEffect(Unit) {
        delay(7_000)
        mePresence.readStoriesUntilNow()
    }

    suspend fun reload() {
        if (geo != null) {
            api.stories(
                geo = geo.toGeo(),
                public = true,
                onError = {
                    if (it is CancellationException) {
                        // Ignored, geo probably changes
                    } else {
                        isLoading = false
                        context.showDidntWork()
                    }
                }
            ) {
                stories = it
                isLoading = false
            }
        }
    }

    suspend fun loadPeople(loadMore: Boolean = false) {
        if (geo != null) {
            if (loadMore) {
                peopleOffset += 20
            } else {
                peopleOffset = 0
                isPeopleLoading = true
            }

            api.people(
                search = peopleSearchText.notBlank,
                geo = geo.toList(),
                offset = peopleOffset,
                limit = 20,
                onError = {
                    if (it is CancellationException) {
                        // Ignored, geo probably changes
                    } else {
                        isPeopleLoading = false
                        context.showDidntWork()
                    }
                }
            ) { result ->
                if (loadMore) {
                    people = people + result
                    hasMorePeople = result.isNotEmpty()
                } else {
                    people = result
                    hasMorePeople = result.size >= 20
                }
                isPeopleLoading = false
            }
        }
    }

    LaunchedEffect(geo) {
        reload()
    }

    // Filter people based on search text
    val filteredPeople = remember(people, peopleSearchText) {
        if (peopleSearchText.isBlank()) {
            people
        } else {
            people.filter { person ->
                person.person.name?.contains(peopleSearchText, ignoreCase = true) == true ||
                person.profile.about?.contains(peopleSearchText, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(geo, sheetContent, peopleSearchText) {
        if (sheetContent == SheetContent.People) {
            loadPeople()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when (sheetContent) {
                SheetContent.Posts -> {
                    StoryContents(
                        source = null,
                        content = storyContents,
                        horizontalPadding = 1.pad,
                        state = state,
                        onReloadRequest = {
                            scope.launch {
                                reload()
                            }
                        },
                        onCommentFocused = {
                            showShareAThought = !it
                        },
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .weight(1f),
                        bottomContentPadding = 80.dp,
                        header = {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SheetHeader(
                                    title = title,
                                    distance = distance,
                                    hint = hint,
                                    onTitleClick = onTitleClick,
                                    selected = sheetContent,
                                    onSelected = { sheetContent = it },
                                    onExpandRequest = onExpandRequest
                                )
                            }

                            if (isLoading) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Loading(
                                        modifier = Modifier
                                            .padding(1.pad)
                                    )
                                }
                            } else if (storyContents.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyText(
                                        stringResource(R.string.no_stories_to_read)
                                    )
                                }
                            }
                        }
                    ) { storyId ->
                        Row {
                            StoryActions(
                                storyId,
                                stories.find { it.id == storyId },
                                showOpen = true
                            )
                        }
                    }
                }

                SheetContent.Pages -> {
                    CardList(
                        state = cardsState,
                        cards = mapCardsControl.mapCategoriesControl.cardsOfCategory,
                        geo = myGeo,
                        isLoading = isLoading,
                        isError = mapCardsControl.isError,
                        value = value,
                        valueChange = valueChange,
                        placeholder = stringResource(R.string.search),
                        hasMore = mapCardsControl.hasMore,
                        onLoadMore = {
                            mapCardsControl.loadMore(false)
                        },
                        action = {
                            Icon(Icons.Outlined.Edit, stringResource(R.string.your_cards))
                        },
                        onAction = {
                            nav.appNavigate(AppNav.Me)
                        },
                        header = {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SheetHeader(
                                    title = title,
                                    distance = distance,
                                    hint = hint,
                                    onTitleClick = onTitleClick,
                                    selected = sheetContent,
                                    onSelected = { sheetContent = it },
                                    onExpandRequest = onExpandRequest
                                )
                            }
                        },
                        aboveSearchFieldContent = {
                            SearchContent(
                                locationSelector = locationSelector,
                                isLoading = isLoading,
                                filters = filters,
                                categories = mapCardsControl.mapCategoriesControl.categories,
                                category = mapCardsControl.mapCategoriesControl.selectedCategory,
                                onCategory = {
                                    mapCardsControl.mapCategoriesControl.selectCategory(it)
                                }
                            )
                        }
                    )
                }

                SheetContent.Groups -> {
                    GroupsScreen(
                        geo = geo?.toGeo(),
                        state = groupsState,
                        locationSelector = locationSelector,
                        header = {
                            item {
                                SheetHeader(
                                    title = title,
                                    distance = distance,
                                    hint = hint,
                                    onTitleClick = onTitleClick,
                                    selected = sheetContent,
                                    onSelected = { sheetContent = it },
                                    onExpandRequest = onExpandRequest
                                )
                            }
                        }
                    )
                }

                SheetContent.Events -> {
                    EventsScreen(
                        geo = geo?.toGeo(),
                        locationSelector = locationSelector,
                        header = {
                            item {
                                SheetHeader(
                                    title = title,
                                    distance = distance,
                                    hint = hint,
                                    onTitleClick = onTitleClick,
                                    selected = sheetContent,
                                    onSelected = { sheetContent = it },
                                    onExpandRequest = onExpandRequest
                                )
                            }
                        }
                    )
                }

                SheetContent.People -> {
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            state = peopleState,
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(1.pad),
                            verticalArrangement = Arrangement.spacedBy(1.pad),
                            contentPadding = PaddingValues(
                                start = 1.pad,
                                top = 0.dp,
                                end = 1.pad,
                                bottom = 3.5f.pad + 80.dp
                            )
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SheetHeader(
                                    title = title,
                                    distance = distance,
                                    hint = hint,
                                    onTitleClick = onTitleClick,
                                    selected = sheetContent,
                                    onSelected = { sheetContent = it },
                                    onExpandRequest = onExpandRequest
                                )
                            }

                            if (isPeopleLoading && people.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Loading(
                                        modifier = Modifier
                                            .padding(1.pad)
                                    )
                                }
                            } else if (people.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyText(
                                        stringResource(R.string.no_people_nearby)
                                    )
                                }
                            } else {
                                items(filteredPeople) { person ->
                                    ProfileCard(person) {
                                        person.person.id?.let { AppNav.Profile(it) }?.let { nav.appNavigate(it) }
                                    }
                                }

                                if (hasMorePeople) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        LaunchedEffect(Unit) {
                                            loadPeople(loadMore = true)
                                        }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            if (isPeopleLoading) {
                                                Loading(
                                                    modifier = Modifier
                                                        .padding(2.pad)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PageInput(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                        ) {
                            SearchFieldAndAction(
                                value = peopleSearchText,
                                valueChange = { peopleSearchText = it },
                                placeholder = stringResource(R.string.search)
                            )
                        }
                    }
                }
            }
        }

        var thought by rememberStateOf("")

        AnimatedVisibility(
            visible = showShareAThought && sheetContent == SheetContent.Posts,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            PageInput {
                SearchFieldAndAction(
                    value = thought,
                    valueChange = { thought = it },
                    placeholder = stringResource(R.string.share_a_thought),
                    showClear = false,
                    singleLine = false,
                    action = {
                        if (thought.isBlank()) {
                            Icon(Icons.Outlined.Edit, stringResource(R.string.your_stories))
                        } else {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, stringResource(R.string.write_a_story))
                        }
                    },
                    onAction = {
                        if (thought.isBlank()) {
                            nav.appNavigate(AppNav.Write)
                        } else {
                            scope.launch {
                                api.createStory(Story(title = thought)) {
                                    nav.appNavigate(AppNav.WriteStory(it.id!!))
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
