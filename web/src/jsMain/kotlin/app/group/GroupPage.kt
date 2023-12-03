package app.group

import GroupLayout
import androidx.compose.runtime.*
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
import org.jetbrains.compose.web.css.*
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
                        GroupList(groups) {
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
        TopBarSearch(search, { search = it})
    } else if (nav is GroupNav.Selected) {
        if (isLoading) {
            Loading()
        } else {
            GroupLayout(nav.group, onGroupUpdated, onGroupGone)
        }
    }
}
