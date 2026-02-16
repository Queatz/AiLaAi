package app.group

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
fun GroupSidePanel(group: GroupExtended, onUpdated: (GroupExtended) -> Unit) {
    val scope = rememberCoroutineScope()
    Div({
        classes(Styles.pane, Styles.sidePane)
        style {
            padding(1.r)
            boxSizing("border-box")
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        val content = group.group?.content?.let {
            try {
                json.decodeFromString<GroupContentModel>(it)
            } catch (e: Exception) {
                null
            }
        }
        if (content != null && content !is GroupContentModel.None) {
            val (icon, title) = when (content) {
                is GroupContentModel.Text -> "text_fields" to "Text"
                is GroupContentModel.Tasks -> "check_box" to "Tasks"
                is GroupContentModel.Card -> "description" to "Card"
                is GroupContentModel.Script -> "code" to "Script"
                is GroupContentModel.Website -> "public" to "Website"
                else -> "" to ""
            }

            IconButton(
                name = "close",
                title = title,
                text = title,
                isReversed = true,
                styles = {
                    margin(.5.r, 0.r)
                    justifyContent(JustifyContent.SpaceBetween)
                }
            ) {
                scope.launch {
                    api.updateGroup(group.group!!.id!!, com.queatz.db.Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.None))) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
        }

        GroupContent(group, onUpdated)
    }
}
