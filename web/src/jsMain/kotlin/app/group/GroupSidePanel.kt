package app.group

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.updateGroup
import app.dialog.dialog
import application
import com.queatz.db.GroupExtended
import components.IconButton
import json
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import r
import com.queatz.db.GroupContent as GroupContentModel

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun GroupSidePanel(
    group: GroupExtended,
    isSwapped: Boolean = false,
    onSwap: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    onUpdated: (GroupExtended) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dynamicTitle by remember(group.group?.id) { mutableStateOf<String?>(null) }
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
                is GroupContentModel.None -> "" to ""
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
                        if (content is GroupContentModel.Text) {
                            if (dialog(application.appString { notesWillBeLost }) != true) {
                                return@launch
                            }
                        } else {
                            if (dialog(application.appString { removeGroupPanel }) != true) {
                                return@launch
                            }
                        }

                        api.updateGroup(
                            id = group.group!!.id!!,
                            groupUpdate = com.queatz.db.Group(
                                content = json.encodeToString<GroupContentModel>(GroupContentModel.None)
                            )
                        ) {
                            onUpdated(group.apply { this.group!!.content = it.content })
                            onClose?.invoke()
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
