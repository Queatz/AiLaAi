package app.group

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.AppNavigation
import app.NavPage
import app.appNav
import components.Icon
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

// Data class to hold feature button information
data class FeatureButtonInfo(
    val icon: String,
    val title: String,
    val createTitle: String,
    val description: String,
    val targetPage: NavPage
)

@Composable
fun FeatureButton(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Div({
        classes(Styles.featureButton)
        onClick {
            onClick()
        }
    }) {
        // Icon
        Div({
            style {
                fontSize(36.px)
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.Center)
                alignItems(AlignItems.Center)
                marginBottom(0.5.r)
            }
        }) {
            Icon(icon)
        }

        // Title
        Div({
            style {
                fontSize(18.px)
                fontWeight("bold")
                textAlign("center")
                marginBottom(0.5.r)
            }
        }) {
            Text(title)
        }

        // Description
        Div({
            style {
                fontSize(14.px)
                textAlign("center")
                opacity(.5f)
            }
        }) {
            Text(description)
        }
    }
}

@Composable
fun FeaturePreview(
    pages: List<NavPage> = emptyList(),
    center: Boolean = true,
    create: Boolean = true
) {
    val scope = rememberCoroutineScope()

    // Define all available feature buttons
    val allFeatureButtons = listOf(
        FeatureButtonInfo(
            icon = "note_add",
            title = "Pages",
            createTitle = "Create a page",
            description = "Create a new page for your content",
            targetPage = NavPage.Cards
        ),
        FeatureButtonInfo(
            icon = "calendar_month",
            title = "Schedule",
            createTitle = "Schedule",
            description = "Manage your schedule and events",
            targetPage = NavPage.Schedule
        ),
        FeatureButtonInfo(
            icon = "person",
            title = "Profile",
            createTitle = "Profile",
            description = "View and edit your profile",
            targetPage = NavPage.Profile
        ),
        FeatureButtonInfo(
            icon = "post_add",
            title = "Posts",
            createTitle = "Write a post",
            description = "Share your thoughts with others",
            targetPage = NavPage.Stories
        ),
        FeatureButtonInfo(
            icon = "description",
            title = "Scripts",
            createTitle = "Create a script",
            description = "Create a script to automate tasks",
            targetPage = NavPage.Scripts
        ),
        FeatureButtonInfo(
            icon = "explore",
            title = "Explore",
            createTitle = "Explore",
            description = "Discover new content and people",
            targetPage = NavPage.Cards
        ),
        FeatureButtonInfo(
            icon = "movie",
            title = "Scenes",
            createTitle = "Create a scene",
            description = "Create a new interactive scene",
            targetPage = NavPage.Scenes
        )
    )

    // Filter buttons based on pages parameter if it's not empty
    val buttonsToShow = if (pages.isNotEmpty()) {
        allFeatureButtons.filter { it.targetPage in pages }
    } else {
        allFeatureButtons
    }

    // TODO Add AI What do you want to do today? input

    // Feature Preview Div
    Div({
        style {
            height(100.percent)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
            if (center) {
                justifyContent(JustifyContent.Center)
            }
            padding(1.r)
        }
    }) {
        // todo: translate all
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fill, minmax(200px, 1fr))")
                property("grid-gap", "1rem")
                padding(2.r)
                width(100.percent)
                maxWidth(800.px)
                maxHeight(100.percent)
                overflowY("auto")
            }
        }) {
            // Render buttons using a for loop
            for (buttonInfo in buttonsToShow) {
                val displayTitle = if (create) {
                    buttonInfo.createTitle
                } else {
                    buttonInfo.title
                }

                FeatureButton(
                    icon = buttonInfo.icon,
                    title = displayTitle,
                    description = buttonInfo.description,
                    onClick = {
                        scope.launch {
                            appNav.navigate(AppNavigation.Nav(buttonInfo.targetPage))
                        }
                    }
                )
            }
        }
    }
}
