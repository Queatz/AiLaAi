package app.page

import Strings.save
import Styles
import androidx.compose.runtime.*
import api
import app.FullPageLayout
import app.PageTopBar
import app.components.TopBarSearch
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.Menu
import app.nav.StoryNav
import appString
import appText
import application
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.api.updateStory
import com.queatz.db.GroupExtended
import com.queatz.db.Story
import com.queatz.db.StoryContent
import com.queatz.db.asGeo
import com.queatz.db.isPart
import com.queatz.db.toJsonStoryPart
import components.Loading
import defaultGeo
import json
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonArray
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import qr
import r
import stories.*
import webBaseUrl

@Composable
fun StoriesPage(
    selected: StoryNav,
    onStoryUpdated: (Story) -> Unit,
    onGroupClick: (GroupExtended) -> Unit
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    var storyContent by remember(selected) { mutableStateOf<List<StoryContent>>(emptyList()) }
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
                Story(
                    content = json.encodeToString(
                        buildJsonArray {
                            storyContent
                                .filter { it.isPart() }
                                .forEach { part ->
                                    add(part.toJsonStoryPart(json))
                                }
                        }
                    )
                )
            ) {
                edited = false
//                onStoryUpdated(it)
            }
        }
    }

    LaunchedEffect(selected, search) {
        isLoading = true

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
                            titleString,
                            "",
                            update,
                            defaultValue = story!!.title ?: ""
                        )

                        if (title == null) return@launch

                        api.updateStory(
                            story.id!!,
                            Story(title = title)
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
            Div({
                classes(Styles.cardContent)
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    padding(1.r)
                }
            }) {
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
                    StoryContents(
                        storyContent,
                        onGroupClick,
                        openInNewWindow = true,
                        editable = (selected as? StoryNav.Selected)?.story?.let {
                            it.person == me?.id && it.published != true
                        } ?: false,
                        onEdited = { edited = true }
                    ) {
                        (selected as? StoryNav.Selected)?.story?.let { story ->
                            save(story)
                        }
                    }
                }
            }
        }
    }
    if (selected is StoryNav.Selected) {
        PageTopBar(
            "",
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
    } else {
        TopBarSearch(search, { search = it })
    }
}
