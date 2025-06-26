package app.nav

import androidx.compose.runtime.*
import api
import app.AppStyles
import app.components.Empty
import app.components.Spacer
import app.dialog.inputDialog
import app.menu.Menu
import appString
import appText
import application
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.myStories
import com.queatz.db.Story
import com.queatz.db.StoryContent
import com.queatz.db.toJsonStoryContent
import components.IconButton
import components.LazyColumn
import components.Loading
import focusable
import json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import stories.storyStatus
import stories.textContent

@kotlinx.serialization.Serializable
sealed class StoryNav {
    @kotlinx.serialization.Serializable
    data object Friends : StoryNav()

    @kotlinx.serialization.Serializable
    data object Local : StoryNav()

    @kotlinx.serialization.Serializable
    data object Saved : StoryNav()

    @kotlinx.serialization.Serializable
    data class Selected(val story: Story) : StoryNav()
}

enum class StoryFilter {
    Published,
    Draft
}

@Composable
fun StoriesNavPage(
    storyUpdates: Flow<Story>,
    selected: StoryNav,
    onSelected: (StoryNav) -> Unit,
    onProfileClick: () -> Unit
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var myStories by remember { mutableStateOf(emptyList<Story>()) }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var searchText by remember(showSearch) {
        mutableStateOf("")
    }

    var filterMenuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }
    var filters by remember {
        mutableStateOf(emptySet<StoryFilter>())
    }
    if (filterMenuTarget != null) {
        Menu(
            onDismissRequest = {
                filterMenuTarget = null
            },
            target = filterMenuTarget!!
        ) {
            item(appString { published }, icon = if (StoryFilter.Published in filters) "check" else null) {
                if (StoryFilter.Published in filters) {
                    filters -= StoryFilter.Published
                } else {
                    filters -= StoryFilter.Draft
                    filters += StoryFilter.Published
                }
            }
            item(appString { draft }, icon = if (StoryFilter.Draft in filters) "check" else null) {
                if (StoryFilter.Draft in filters) {
                    filters -= StoryFilter.Draft
                } else {
                    filters -= StoryFilter.Published
                    filters += StoryFilter.Draft
                }
            }

        }
    }

    LaunchedEffect(selected) {
        searchText = ""
        showSearch = false
    }

    val shownStories = remember(myStories, searchText, filters) {
        val search = searchText.trim()
        if (searchText.isBlank()) {
            myStories
        } else {
            myStories.filter {
                (it.title?.contains(search, true) ?: false)
            }
        }.let {
            if (filters.isEmpty()) {
                it
            } else {
                it.filter { story ->
                    filters.all { filter ->
                        when (filter) {
                            StoryFilter.Published -> story.published == true
                            StoryFilter.Draft -> story.published != true
                        }
                    }
                }
            }
        }
    }

    suspend fun reload() {
        api.myStories {
            myStories = it
        }

        (selected as? StoryNav.Selected)?.story?.let { selected ->
            onSelected(myStories.firstOrNull { it.id == selected.id }?.let { StoryNav.Selected(it) }
                ?: StoryNav.Friends)
        }

        isLoading = false
    }

    LaunchedEffect(Unit) {
        reload()
    }

    // todo see youtrack, should not need (selected)
    LaunchedEffect(selected) {
        storyUpdates.collectLatest {
            reload()
        }
    }

    NavTopBar(me, appString { posts }, onProfileClick) {
        IconButton("search", appString { search }, styles = {
        }) {
            showSearch = !showSearch
        }

        IconButton("filter_list", appString { filter }, count = filters.size, styles = {
        }) {
            filterMenuTarget =
                if (filterMenuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
        }

        val createStory = appString { createStory }
        val title = appString { title }
        val create = appString { create }

        IconButton("add", appString { this.createStory }, styles = {
            marginRight(.5.r)
        }) {
            scope.launch {
                val title = inputDialog(
                    createStory,
                    title,
                    create
                )
                if (title == null) return@launch
                api.createStory(Story(title = title, content = listOf(StoryContent.Section(""), StoryContent.Text("")).toJsonStoryContent(json))) {
                    reload()
                }
            }
        }
    }
    if (showSearch) {
        NavSearchInput(searchText, { searchText = it }, onDismissRequest = {
            searchText = ""
            showSearch = false
        })
    }
    // todo this is same as groupsnavpage Should be NavMainContent
    if (isLoading) {
        Loading()
    } else {
        Div({
            style {
                overflowY("auto")
                overflowX("hidden")
                padding(1.r / 2)
            }
        }) {
            if (!showSearch) {
                NavMenuItem("group", appString { friends }, selected = selected is StoryNav.Friends) {
                    onSelected(StoryNav.Friends)
                }
                NavMenuItem("location_on", appString { explore }, selected = selected is StoryNav.Local) {
                    onSelected(StoryNav.Local)
                }
//                NavMenuItem("favorite", "Saved", selected = false) {
//                    onSelected(null)
//                }
                Spacer()
            }
            if (shownStories.isEmpty()) {
                Empty {
                    appText { noStories }
                }
            } else {
                LazyColumn {
                    items(shownStories) {
                        StoryItem(it, it == (selected as? StoryNav.Selected)?.story) {
                            onSelected(
                                StoryNav.Selected(
                                    it
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryItem(
    story: Story,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Div({
        classes(
            listOf(AppStyles.groupItem) + if (selected) {
                listOf(AppStyles.groupItemSelected)
            } else {
                emptyList()
            }
        )
        focusable()
        onClick {
            onSelected()
        }
    }) {
        Div({
            style {
                width(0.px)
                flexGrow(1)
            }
        }) {
            Div({
                classes(AppStyles.groupItemName)
            }) {
                Text(story.title?.notBlank ?: appString { newStory })
            }
            Div({
                classes(AppStyles.groupItemMessage)
            }) {
                Text(storyStatus(story.publishDate))
            }
            Div({
                classes(AppStyles.groupItemMessage)
            }) {
                Text(story.textContent())
            }
        }
    }
}
