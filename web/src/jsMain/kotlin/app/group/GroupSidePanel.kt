package app.group

import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.updateGroup
import json
import com.queatz.db.GroupContent as GroupContentModel
import com.queatz.db.GroupExtended
import components.Icon
import components.IconButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun GroupSidePanel(
    group: GroupExtended,
    isSwapped: Boolean = false,
    onSwap: () -> Unit = {},
    onUpdated: (GroupExtended) -> Unit
) {
    val scope = rememberCoroutineScope()
    var dynamicTitle by remember(group) { mutableStateOf<String?>(null) }
    Div({
        classes(Styles.pane)
        if (isSwapped) {
            classes(Styles.flexGrow)
        } else {
            classes(Styles.sidePane)
        }
        style {
            padding(1.r)
            boxSizing("border-box")
        }
    }) {
        val content = group.group?.content?.let {
            try {
                json.decodeFromString<GroupContentModel>(it)
            } catch (e: Exception) {
                null
            }
        }

        LaunchedEffect(content) {
            dynamicTitle = null
        }

        if (content != null && content !is GroupContentModel.None) {
            val (icon, defaultTitle) = when (content) {
                is GroupContentModel.Text -> "text_fields" to "Notes"
                is GroupContentModel.Tasks -> "check_box" to "Tasks"
                is GroupContentModel.Card -> "description" to "Page"
                is GroupContentModel.Script -> "code" to "Script"
                is GroupContentModel.Website -> "public" to "Website"
                else -> "" to ""
            }

            val title = dynamicTitle ?: defaultTitle

            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                }
            }) {
                IconButton(
                    name = "close",
                    title = title,
                    text = title,
                    isReversed = true,
                    styles = {
                        flex(1)
                        margin(.5.r, 0.r)
                        justifyContent(JustifyContent.SpaceBetween)
                    }
                ) {
                    scope.launch {
                        api.updateGroup(
                            id = group.group!!.id!!,
                            groupUpdate = com.queatz.db.Group(
                                content = json.encodeToString<GroupContentModel>(GroupContentModel.None)
                            )
                        ) {
                            onUpdated(group.apply { this.group!!.content = it.content })
                        }
                    }
                }

                IconButton(
                    name = "swap_horiz",
                    title = "Swap positions",
                    styles = {
                        margin(.5.r, 0.r, .5.r, .5.r)
                    }
                ) {
                    onSwap()
                }
            }
        }

        GroupContent(group, onUpdated) {
            dynamicTitle = it
        }
    }
}
