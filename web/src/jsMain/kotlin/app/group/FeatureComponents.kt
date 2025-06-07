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
import api
import app.ailaai.api.newCard
import app.ailaai.api.updateCard
import com.queatz.db.Card
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.SpaceData
import com.queatz.widgets.widgets.PageTreeData
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.ImpactEffortTableData
import createWidget
import json
import com.queatz.db.StoryContent
import kotlinx.serialization.json.buildJsonArray
import com.queatz.db.toJsonStoryPart

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
        ),
        FeatureButtonInfo(
            icon = "space_dashboard",
            title = "Spaces",
            createTitle = "Create a space",
            description = "Create a sketch or presentation",
            targetPage = NavPage.Cards
        ),
        FeatureButtonInfo(
            icon = "account_tree",
            title = "Page Trees",
            createTitle = "Create a page tree",
            description = "Organize small projects and track progress",
            targetPage = NavPage.Cards
        ),
        FeatureButtonInfo(
            icon = "list_alt",
            title = "Forms",
            createTitle = "Create a form",
            description = "Collect and organize responses",
            targetPage = NavPage.Cards
        ),
        FeatureButtonInfo(
            icon = "table_chart",
            title = "Impact-Effort Tables",
            createTitle = "Create an impact-effort table",
            description = "Prioritize tasks based on impact and effort",
            targetPage = NavPage.Cards
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
                            when (buttonInfo.createTitle) {
                                "Create a space" -> {
                                    api.newCard(Card(name = "New Space")) { newCard ->
                                        api.createWidget(
                                            widget = Widgets.Space,
                                            data = json.encodeToString(SpaceData(card = newCard.id!!))
                                        ) { widget ->
                                            val contentJson = buildJsonArray {
                                                add(StoryContent.Widget(widget.widget!!, widget.id!!).toJsonStoryPart(json))
                                            }
                                            api.updateCard(
                                                id = newCard.id!!,
                                                card = Card(content = json.encodeToString(contentJson))
                                            ) {
                                                appNav.navigate(AppNavigation.Page(newCard.id!!, newCard))
                                            }
                                        }
                                    }
                                }
                                "Create a page tree" -> {
                                    api.newCard(Card(name = "New Page Tree")) { newCard ->
                                        api.createWidget(
                                            widget = Widgets.PageTree,
                                            data = json.encodeToString(PageTreeData(card = newCard.id!!))
                                        ) { widget ->
                                            val contentJson = buildJsonArray {
                                                add(StoryContent.Widget(widget.widget!!, widget.id!!).toJsonStoryPart(json))
                                            }
                                            api.updateCard(
                                                id = newCard.id!!,
                                                card = Card(content = json.encodeToString(contentJson))
                                            ) {
                                                appNav.navigate(AppNavigation.Page(newCard.id!!, newCard))
                                            }
                                        }
                                    }
                                }
                                "Create a form" -> {
                                    api.newCard(Card(name = "New Form")) { newCard ->
                                        api.createWidget(
                                            widget = Widgets.Form,
                                            data = json.encodeToString(FormData(page = newCard.id!!))
                                        ) { widget ->
                                            val contentJson = buildJsonArray {
                                                add(StoryContent.Widget(widget.widget!!, widget.id!!).toJsonStoryPart(json))
                                            }
                                            api.updateCard(
                                                id = newCard.id!!,
                                                card = Card(content = json.encodeToString(contentJson))
                                            ) {
                                                appNav.navigate(AppNavigation.Page(newCard.id!!, newCard))
                                            }
                                        }
                                    }
                                }
                                "Create an impact-effort table" -> {
                                    api.newCard(Card(name = "New Impact-Effort Table")) { newCard ->
                                        api.createWidget(
                                            widget = Widgets.ImpactEffortTable,
                                            data = json.encodeToString(ImpactEffortTableData(card = newCard.id!!))
                                        ) { widget ->
                                            val contentJson = buildJsonArray {
                                                add(StoryContent.Widget(widget.widget!!, widget.id!!).toJsonStoryPart(json))
                                            }
                                            api.updateCard(
                                                id = newCard.id!!,
                                                card = Card(content = json.encodeToString(contentJson))
                                            ) {
                                                appNav.navigate(AppNavigation.Page(newCard.id!!, newCard))
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    appNav.navigate(AppNavigation.Nav(buttonInfo.targetPage))
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
