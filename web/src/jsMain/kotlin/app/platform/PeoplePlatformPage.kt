package app.platform

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.FullPageLayout
import app.ailaai.api.platformLatestMembers
import appString
import com.queatz.db.Person
import components.LazyColumn
import components.Loading
import components.ProfilePhoto
import focusable
import format
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.datetime.toJSDate
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun PeoplePlatformPage() {
    var members by remember {
        mutableStateOf(emptyList<Person>())
    }
    var isLoading by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun loadMore() {
        if (isLoading || !hasMore) return
        isLoading = true
        api.platformLatestMembers(
            offset = offset,
            limit = 20
        ) {
            if (it.isEmpty()) {
                hasMore = false
            } else {
                members = members + it
                offset += it.size
            }
        }
        isLoading = false
    }

    // Kotlin/JS hack
    var shownMembers = remember(members) {
        members
    }

    LaunchedEffect(Unit) {
        loadMore()
    }

    if (isLoading && shownMembers.isEmpty()) {
        Loading()
    } else {
        FullPageLayout {
            LazyColumn {
                item {
                    // todo: translate
                    H3 {
                        Text("Latest members")
                    }
                }
                items(shownMembers) { person ->
                    Div(
                        {
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                gap(1.r)
                            }
                        }
                    ) {
                        Div({
                            classes(AppStyles.groupItem)

                            style {
                                display(DisplayStyle.Flex)
                                gap(1.r)
                            }

                            focusable()

                            onClick {
                                window.open("/profile/${person.id}", target = "_blank")
                            }
                        }) {
                            ProfilePhoto(person)
                            Div {
                                Div({
                                    classes(AppStyles.groupItemName)
                                }) {
                                    Text(person.name ?: appString { someone })
                                }
                                Div({
                                    classes(AppStyles.groupItemMessage)
                                }) {
                                    Text(person.createdAt?.toJSDate()?.format().orEmpty())
                                }
                            }
                        }
                    }
                }

                item {
                    Div(
                        {
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Row)
                                alignItems(AlignItems.Center)
                                justifyContent(JustifyContent.Center)
                                padding(1.r)
                            }
                        }
                    ) {
                        Button({
                            classes(Styles.button)

                            onClick {
                                scope.launch { loadMore() }
                            }
                        }) {
                            Text("Load more")
                        }
                    }
                }
            }
        }
    }
}
