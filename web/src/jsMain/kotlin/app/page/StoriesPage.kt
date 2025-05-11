package app.page

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.FullPageLayout
import app.PageTopBar
import app.components.EditField
import app.components.TopBarSearch
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.Menu
import app.nav.StoryNav
import appString
import appText
import application
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.api.updateStory
import com.queatz.db.GroupExtended
import com.queatz.db.Story
import com.queatz.db.StoryContent
import com.queatz.db.asGeo
import com.queatz.db.toJsonStoryContent
import components.Loading
import defaultGeo
import json
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import qr
import r
import stories.StoryContents
import stories.asTextContent
import stories.full
import webBaseUrl

@Composable
fun StoriesPage(
    selected: StoryNav,
    onStoryUpdated: (Story) -> Unit,
    onGroupClick: (GroupExtended) -> Unit
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    var storyContent by remember { mutableStateOf<List<StoryContent>>(emptyList()) }
    var isLoading by remember {
        mutableStateOf(true)
    }
    var edited by remember(selected) {
        mutableStateOf(false)
    }
    var menuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }
    var search by remember {
        mutableStateOf("")
    }

    fun save(story: Story) {
        scope.launch {
            api.updateStory(
                story.id!!,
                Story(content = storyContent.toJsonStoryContent(json))
            ) {
                edited = false
//                onStoryUpdated(it)
            }
        }
    }

    suspend fun reload() {
        fun List<Story>.search() = if (search.isBlank()) this else filter {
            it.asTextContent().lowercase().contains(search.trim().lowercase())
        }

        when (selected) {
            is StoryNav.Friends -> {
                api.stories(me?.geo?.asGeo() ?: defaultGeo) { stories ->
                    storyContent = stories.search().flatMapIndexed { index, it ->
                        if (index < stories.lastIndex) it.full() + StoryContent.Divider else it.full()
                    }
                }
            }

            is StoryNav.Local -> {
                api.stories(me?.geo?.asGeo() ?: defaultGeo, public = true) { stories ->
                    storyContent = stories.search().flatMapIndexed { index, it ->
                        if (index < stories.lastIndex) it.full() + StoryContent.Divider else it.full()
                    }
                }
            }
            is StoryNav.Saved -> {}
            is StoryNav.Selected -> {
                storyContent = selected.story.full()
            }
        }
    }

    LaunchedEffect(selected, search) {
        isLoading = true

        reload()

        isLoading = false
    }

    (selected as? StoryNav.Selected)?.story?.let { story ->
        menuTarget?.let { target ->
            Menu({ menuTarget = null }, target) {
                item(appString { openInNewTab }, icon = "open_in_new") {
                    window.open("/story/${story.id!!}", target = "_blank")
                }

                val titleString = appString { title }
                val update = appString { update }

                item(appString { rename }) {
                    scope.launch {
                        val title = inputDialog(
                            title = titleString,
                            placeholder = "",
                            confirmButton = update,
                            defaultValue = story.title ?: ""
                        )

                        if (title == null) return@launch

                        api.updateStory(
                            id = story.id!!,
                            story = Story(title = title)
                        ) {
                            onStoryUpdated(it)
                        }
                    }
                }

                item(appString { qrCode }) {
                    scope.launch {
                        dialog("", cancelButton = null) {
                            val qrCode = remember {
                                "$webBaseUrl/story/${story.id!!}".qr
                            }
                            Img(src = qrCode) {
                                style {
                                    borderRadius(1.r)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isLoading) {
        Loading()
    } else {
        FullPageLayout {
            TopBarSearch(
                value = search,
                onValue = { search = it }
            )
            Div({
                classes(Styles.cardContent)
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    padding(1.r)
                }
            }) {
                var newPostText by remember { mutableStateOf("") }

                if (selected is StoryNav.Friends && search.isEmpty()) {
                    Div(
                        {
                            style {
                                width(100.percent)
                            }
                        }
                    ) {
                        EditField(
                            value = newPostText,
                            placeholder = appString { shareAThought },
                            autoFocus = true,
                            resetOnSubmit = true,
                            showDiscard = false,
                            button = appString { post },
                            buttonBarStyles = {
                                justifyContent(JustifyContent.End)
                                width(100.percent)
                            },
                            styles = {
                                width(100.percent)
                                minHeight(9.r)
                            }
                        ) { title ->
                            var success = false
                            api.createStory(
                                Story(
                                    title = title
                                )
                            ) { story ->
                                api.updateStory(
                                    id = story.id!!,
                                    story = Story(published = true)
                                ) {
                                    reload()
                                    onStoryUpdated(it)
                                    success = true
                                }
                            }
                            success
                        }
                    }
                }

                if (storyContent.isEmpty()) {
                    Div({
                        style {
                            color(Styles.colors.secondary)
                            padding(1.r)
                            width(100.percent)
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            alignItems(AlignItems.Center)
                        }
                    }) {
                        appText { noStories }
                    }
                } else {
                    val editable = (selected as? StoryNav.Selected)?.story?.let {
                        it.person == me?.id && it.published != true
                    } == true
                    StoryContents(
                        content = storyContent,
                        onGroupClick = onGroupClick,
                        openInNewWindow = true,
                        editable = editable,
                        onEdited = { index, part ->
                            storyContent = storyContent.toMutableList().apply {
                                set(index, part)
                            }
                            edited = true
                        }
                    ) {
                        (selected as? StoryNav.Selected)?.story?.let { story ->
                            save(story)
                        }
                    }
                    if (editable) {
                       // todo: add section
                    }
                }
            }
        }
    }
    if (selected is StoryNav.Selected) {
        PageTopBar(
            title = "",
//                story.title?.notBlank ?: "New story"
            actions = {
                if (edited) {
                    Button({
                        classes(Styles.button)

                        style {
                            height(100.percent)
                        }

                        onClick {
                            save(selected.story)
                        }
                    }) {
                        Text(appString { save })
                    }
                }
            }
        ) {
            menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
        }
    }
}
