package components

import Styles
import androidx.compose.runtime.*
import api
import app.AppNavigation
import app.appNav
import appString
import com.queatz.ailaai.api.storyByUrl
import com.queatz.db.Story
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import stories.StoryContent
import stories.StoryContents
import stories.full

@Composable
fun StoryPage(storyUrl: String, onStoryLoaded: (Story) -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var story by remember { mutableStateOf<Story?>(null) }
    var storyContent by remember { mutableStateOf<List<StoryContent>?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(storyUrl) {
        isLoading = true
        api.storyByUrl(storyUrl) {
            story = it
            onStoryLoaded(story!!)
        }
        isLoading = false
    }

    LaunchedEffect(story) {
        story?.let { story ->
            storyContent = story.full()
        }
    }

    if (!isLoading && story == null) {
        Div({
            classes(Styles.mainContent)
            style {
                display(DisplayStyle.Flex)
                minHeight(100.vh)
                width(100.percent)
                flexDirection(FlexDirection.Column)
                padding(2.r)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.FlexStart)
            }
        }) {
            Text(appString { storyNotFound })
        }
    } else {
        story?.let { story ->
            Div({
                classes(Styles.mainContent)
            }) {
                Div({
                    classes(Styles.navContainer)
                    style {
                        width(100.percent)
                        maxWidth(800.px)
                        alignSelf(AlignSelf.Center)
                        marginBottom(1.r)
                    }
                }) {
                    Div({
                        classes(Styles.navContent)
                    }) {
                        Div({
                            classes(Styles.cardContent)
                        }) {
                            if (storyContent != null) {
                                StoryContents(storyContent!!, onGroupClick = {
                                    scope.launch {
                                        appNav.navigate(AppNavigation.Group(it.group!!.id!!, it))
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
