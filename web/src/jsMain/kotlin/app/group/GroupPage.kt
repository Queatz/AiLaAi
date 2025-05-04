package app.group

import GroupLayout
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
import app.FullPageLayout
import app.NavPage
import app.ailaai.api.createGroup
import app.ailaai.api.exploreGroups
import app.ailaai.api.group
import app.ailaai.api.updateGroup
import app.appNav
import app.components.TopBarSearch
import app.dialog.inputDialog
import app.nav.GroupNav
import app.softwork.routingcompose.Router
import appString
import appText
import application
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.asGeo
import components.Icon
import components.Loading
import components.Tip
import defaultGeo
import format
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

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
                fontSize(24.px)
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
fun GroupPage(
    nav: GroupNav,
    onGroup: (GroupExtended) -> Unit,
    onGroupUpdated: () -> Unit,
    onGroupGone: () -> Unit
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()

    var isLoading by remember {
        mutableStateOf(true)
    }

    var groups by remember {
        mutableStateOf(emptyList<GroupExtended>())
    }

    var search by remember {
        mutableStateOf("")
    }

    LaunchedEffect(nav, search) {
        if (search.isBlank()) {
            groups = emptyList()
        }
        when (nav) {
            GroupNav.Friends -> {
                isLoading = search.isBlank()
                api.exploreGroups(
                    geo = me?.geo?.asGeo() ?: defaultGeo,
                    search = search.notBlank,
                    public = false
                ) {
                    groups = it
                }
                isLoading = false
            }

            GroupNav.Local -> {
                isLoading = search.isBlank()
                api.exploreGroups(
                    geo = me?.geo?.asGeo() ?: defaultGeo,
                    search = search.notBlank,
                    public = true
                ) {
                    groups = it
                }
                isLoading = false
            }

            else -> {
                isLoading = false
            }
        }
    }

    if (nav == GroupNav.None) {
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
                    width(100.percent)
                    maxWidth(800.px)
                    overflow("auto")
                    maxHeight(100.percent)
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
            }
        }
    } else if (nav is GroupNav.Local || nav is GroupNav.Friends) {
        if (isLoading) {
            Loading()
        } else {
            FullPageLayout {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        padding(0.r, 1.r, 1.r, 1.r)
                    }
                }) {
                    if (groups.isNotEmpty()) {
                        GroupList(groups, coverPhoto = true) {
                            onGroup(it)
                        }
                    } else if (search.isNotBlank()) {
                        Tip(
                            text = appString { createOpenGroupAbout }
                                .format(search.trim()),
                            action = appString { createGroup }
                        ) {
                            scope.launch {
                                val result = inputDialog(
                                    title = application.appString { createOpenGroup },
                                    defaultValue = search.trim(),
                                    confirmButton = application.appString { create }
                                )

                                if (result == null) return@launch

                                api.createGroup(emptyList()) { group ->
                                    api.updateGroup(group.id!!, Group(name = result, open = true))
                                    api.group(group.id!!) {
                                        onGroup(it)
                                    }
                                }

                                search = ""
                            }
                        }
                    }
                }
            }
        }
        TopBarSearch(
            value = search,
            onValue = { search = it}
        )
    } else if (nav is GroupNav.Selected) {
        if (isLoading) {
            Loading()
        } else {
            GroupLayout(
                group = nav.group,
                onGroupUpdated = onGroupUpdated,
                onGroupGone = onGroupGone
            )
        }
    }
}
