package app.nav
import androidx.compose.runtime.*
import api
import app.AppStyles
import app.components.Empty
import app.components.Spacer
import app.dialog.inputDialog
import appString
import appText
import application
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.myStories
import com.queatz.db.Story
import components.IconButton
import components.Loading
import focusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import notBlank
import opensavvy.compose.lazy.LazyColumn
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import stories.storyStatus
import stories.textContent

sealed class StoryNav {
    data object Friends : StoryNav()
    data object Local : StoryNav()
    data object Saved : StoryNav()
    data class Selected(val story: Story) : StoryNav()
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

    var searchText by remember {
        mutableStateOf("")
    }

    LaunchedEffect(selected) {
        searchText = ""
        showSearch = false
    }

    val shownStories = remember(myStories, searchText) {
        val search = searchText.trim()
        if (searchText.isBlank()) {
            myStories
        } else {
            myStories.filter {
                (it.title?.contains(search, true) ?: false)
            }
        }
    }

    suspend fun reload() {
        api.myStories {
            myStories = it
        }

        (selected as? StoryNav.Selected)?.story?.let { selected ->
            onSelected(myStories.firstOrNull { it.id == selected.id }?.let { StoryNav.Selected(it) } ?: StoryNav.Friends)
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

    NavTopBar(me, appString { explore }, onProfileClick) {
        IconButton("search", appString { search }, styles = {
        }) {
            showSearch = !showSearch
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
                api.createStory(Story(title = title)) {
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
                property("scrollbar-width", "none")
                padding(1.r / 2)
            }
        }) {
            if (!showSearch) {
                NavMenuItem("group", appString { friends }, selected = selected is StoryNav.Friends) {
                    onSelected(StoryNav.Friends)
                }
                NavMenuItem("location_on", appString { local }, selected = selected is StoryNav.Local) {
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
                key(shownStories, selected) { // todo remove after LazyColumn library is updated
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
}

@Composable
fun StoryItem(story: Story, selected: Boolean, onSelected: () -> Unit) {
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
                Text(story.title?.notBlank ?: appString { createStory })
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
