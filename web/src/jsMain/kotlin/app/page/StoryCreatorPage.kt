package app.page

import LocalConfiguration
import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.FullPageLayout
import app.PageTopBar
import app.dialog.dialog
import app.dialog.inputDialog
import app.dialog.publishStoryDialog
import app.menu.Menu
import appString
import com.queatz.ailaai.api.deleteStory
import com.queatz.ailaai.api.story
import com.queatz.ailaai.api.updateStory
import com.queatz.db.Story
import com.queatz.db.StoryContent
import com.queatz.db.toJsonStoryContent
import components.ContentActions
import components.Loading
import json
import kotlinx.browser.window
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import qr
import r
import stories.StoryContents
import stories.asContents
import webBaseUrl
import org.jetbrains.compose.web.css.borderRadius

@Composable
fun StoryCreatorPage(
    story: Story,
    onStoryUpdated: (Story) -> Unit,
    onStoryDeleted: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    var storyContent by remember { mutableStateOf<List<StoryContent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var edited by remember(story) { mutableStateOf(false) }
    var menuTarget by remember { mutableStateOf<DOMRect?>(null) }

    LaunchedEffect(story.id) {
        if (story.id != null) {
            api.story(
                id = story.id!!
            ) { fullStory ->
                storyContent = fullStory.asContents()
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    fun save() {
        scope.launch {
            api.updateStory(
                id = story.id!!,
                story = Story(
                    content = storyContent.toJsonStoryContent(json)
                )
            ) {
                edited = false
                onStoryUpdated(it)
            }
        }
    }

    val titleString = appString { title }
    val updateString = appString { update }

    fun rename() {
        scope.launch {
            val newTitle = inputDialog(
                title = titleString,
                placeholder = "",
                confirmButton = updateString,
                defaultValue = story.title ?: ""
            )

            if (newTitle == null) return@launch

            api.updateStory(
                id = story.id!!,
                story = Story(title = newTitle)
            ) {
                onStoryUpdated(it)
            }
        }
    }

    menuTarget?.let { target ->
        Menu(
            onDismissRequest = { menuTarget = null },
            target = target
        ) {
            item(appString { openInNewTab }, icon = "open_in_new") {
                window.open("/story/${story.id!!}", target = "_blank")
            }

            item(appString { rename }) {
                rename()
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

            if (story.published != true) {
                item(appString { publish }) {
                    scope.launch {
                        val confirmed = publishStoryDialog(
                            configuration = configuration,
                            story = story,
                            storyContents = storyContent,
                            onStoryUpdated = onStoryUpdated
                        )

                        if (!confirmed) return@launch

                        if (edited) {
                            api.updateStory(
                                id = story.id!!,
                                story = Story(
                                    content = storyContent.toJsonStoryContent(json),
                                    published = true
                                )
                            ) {
                                edited = false
                                onStoryUpdated(it)
                            }
                        } else {
                            api.updateStory(
                                id = story.id!!,
                                story = Story(published = true)
                            ) {
                                onStoryUpdated(it)
                            }
                        }
                    }
                }
            }

            val deleteString = appString { delete }
            val cancelString = appString { cancel }

            item(deleteString, icon = "delete") {
                scope.launch {
                    val confirmed = dialog(
                        title = deleteString,
                        confirmButton = deleteString,
                        cancelButton = cancelString
                    ) {}

                    if (confirmed == true) {
                        api.deleteStory(
                            id = story.id!!
                        ) {
                            onStoryDeleted?.invoke()
                        }
                    }
                }
            }
        }
    }

    FullPageLayout {
        if (isLoading) {
            Loading()
        } else {
            StoryContents(
                content = storyContent,
                editable = true,
                onEdited = { index, part ->
                    storyContent = storyContent.toMutableList().apply { set(index, part) }
                    edited = true
                },
                onReorder = { fromIndex, toIndex ->
                    storyContent = storyContent.toMutableList().apply {
                        val item = removeAt(fromIndex)
                        add(
                            if (fromIndex < toIndex) toIndex - 1 else toIndex,
                            item
                        )
                    }
                    edited = true
                },
                onSave = { newContent ->
                    storyContent = newContent
                    edited = true
                },
                actions = { index, part ->
                    ContentActions(
                        index = index,
                        part = part,
                        isEditable = true,
                        currentContent = storyContent,
                        onContentUpdated = { newList ->
                            storyContent = newList
                            edited = true
                        }
                    )
                }
            )
        }
    }

    StoryCreatorToolbar(
        onContentAdded = { newContent ->
            storyContent = storyContent + newContent
            edited = true
        }
    )

    PageTopBar(
        title = story.title?.notBlank ?: appString { newStory },
        onTitleClick = { rename() },
        actions = {
            if (edited) {
                Button(
                    attrs = {
                        classes(Styles.button)
                        style {
                            height(100.percent)
                        }
                        onClick { save() }
                    }
                ) {
                    Text(appString { save })
                }
            }
        }
    ) {
        menuTarget = if (menuTarget == null) {
            (it.target as HTMLElement).getBoundingClientRect()
        } else {
            null
        }
    }
}
