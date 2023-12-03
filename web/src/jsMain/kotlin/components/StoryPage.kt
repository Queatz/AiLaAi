package components

import Styles
import androidx.compose.runtime.*
import api
import app.softwork.routingcompose.Router
import appString
import com.queatz.ailaai.api.storyByUrl
import com.queatz.db.Story
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
                            val router = Router.current

                            if (storyContent != null) {
                                StoryContents(storyContent!!, onGroupClick = {
                                    // todo if signed in, go to group
                                    router.navigate("/signin")
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
