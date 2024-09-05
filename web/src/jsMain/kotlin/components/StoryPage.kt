package components

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
import app.AppNavigation
import app.appNav
import appString
import application
import baseUrl
import com.queatz.ailaai.api.storyByUrl
import com.queatz.db.Story
import com.queatz.db.StoryContent
import kotlinx.coroutines.launch
import mainContent
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import stories.StoryContents
import stories.full
import webBaseUrl

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun StoryPage(storyUrl: String, onStoryLoaded: (Story) -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var story by remember { mutableStateOf<Story?>(null) }
    var storyContent by remember { mutableStateOf<List<StoryContent>?>(null) }
    val scope = rememberCoroutineScope()

    val layout by application.layout.collectAsState()

    application.background(story?.background?.let { "$baseUrl$it" })

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

    if (layout == AppLayout.Kiosk && story != null) {
        QrImg("$webBaseUrl/story/${story?.id}") {
            position(Position.Fixed)
            bottom(2.r)
            left(2.r)
            maxWidth(10.vw)
            maxHeight(10.vw)
            transform {
                scale(2)
                translate(25.percent, -25.percent)
            }
        }
    }

    if (!isLoading && story == null) {
        Div({
            mainContent(layout)
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
                mainContent(layout)

                style {
                    if (layout == AppLayout.Kiosk) {
                        marginRight(1.r)
                    }
                }
            }) {
                Div({
                    classes(Styles.navContainer)
                    style {
                        width(100.percent)
                        alignSelf(AlignSelf.Center)
                        marginBottom(1.r)
                        if (layout == AppLayout.Default) {
                            maxWidth(800.px)

                            if (story.background.isNullOrBlank()) {
                                property("background", "unset")
                            }
                        }
                    }
                }) {
                    Div({
                        classes(Styles.navContent)
                    }) {
                        Div({
                            classes(Styles.cardContent)
                        }) {
                            if (storyContent != null) {
                                StoryContents(
                                    storyContent!!,
                                    onGroupClick = {
                                        scope.launch {
                                            appNav.appNavigate(AppNavigation.Group(it.group!!.id!!, it))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
