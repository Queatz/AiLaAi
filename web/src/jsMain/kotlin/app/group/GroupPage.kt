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
import app.FullPageLayout
import app.ailaai.api.createGroup
import app.ailaai.api.exploreGroups
import app.ailaai.api.group
import app.ailaai.api.updateGroup
import app.components.TopBarSearch
import app.dialog.inputDialog
import app.nav.GroupNav
import appString
import appText
import application
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.asGeo
import components.Loading
import components.Tip
import defaultGeo
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.dom.Div
import r

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
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.Center)
                opacity(.5)
            }
        }) {
            appText { selectAGroup }
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
                            // todo: translate
                            text = "Create an open group about \"${search.trim()}\".",
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
