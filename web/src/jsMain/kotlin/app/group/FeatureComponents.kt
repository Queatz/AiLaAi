package app.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.AppNavigation
import app.NavPage
import app.appNav
import components.Icon
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import Styles

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
fun FeaturePreview() {
    val scope = rememberCoroutineScope()

    // TODO Add AI What do you want to do today? input

    // Feature Preview Div
    Div({
        style {
            height(100.percent)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.Center)
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
            // Create a page
            FeatureButton(
                icon = "note_add",
                title = "Create a page",
                description = "Create a new page for your content",
                onClick = {
                    scope.launch {
                        appNav.navigate(AppNavigation.Nav(NavPage.Cards))
                    }
                }
            )

            // Schedule
            FeatureButton(
                icon = "calendar_month",
                title = "Schedule",
                description = "Manage your schedule and events",
                onClick = {
                    scope.launch {
                        appNav.navigate(AppNavigation.Nav(NavPage.Schedule))
                    }
                }
            )

            // Profile
            FeatureButton(
                icon = "person",
                title = "Profile",
                description = "View and edit your profile",
                onClick = {
                    scope.launch {
                        appNav.navigate(AppNavigation.Nav(NavPage.Profile))
                    }
                }
            )

            // Write a post
            FeatureButton(
                icon = "post_add",
                title = "Write a post",
                description = "Share your thoughts with others",
                onClick = {
                    scope.launch {
                        appNav.navigate(AppNavigation.Nav(NavPage.Stories))
                    }
                }
            )

            // Create a script
            FeatureButton(
                icon = "description",
                title = "Create a script",
                description = "Create a script to automate tasks",
                onClick = {
                    scope.launch {
                        appNav.navigate(AppNavigation.Nav(NavPage.Scripts))
                    }
                }
            )

            // Explore
            FeatureButton(
                icon = "explore",
                title = "Explore",
                description = "Discover new content and people",
                onClick = {
                    scope.launch {
                        appNav.navigate(AppNavigation.Nav(NavPage.Cards))
                    }
                }
            )

            // Create a scene
            FeatureButton(
                icon = "movie",
                title = "Create a scene",
                description = "Create a new interactive scene",
                onClick = {
                    scope.launch {
                        appNav.navigate(AppNavigation.Nav(NavPage.Scenes))
                    }
                }
            )
        }
    }
}
