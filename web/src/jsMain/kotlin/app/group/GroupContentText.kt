package app.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.updateGroup
import app.components.Empty
import app.components.FlexInput
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.GroupContent as GroupContentModel
import components.Markdown
import json
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun GroupContentText(
    group: GroupExtended,
    content: GroupContentModel.Text,
    onUpdated: (GroupExtended) -> Unit
) {
    var editing by remember { mutableStateOf(content.text.isNullOrBlank()) }
    if (editing) {
        var text by remember { mutableStateOf(content.text ?: "") }
        FlexInput(
            value = text,
            onChange = {
                text = it
            },
            onSubmit = {
                api.updateGroup(
                    group.group!!.id!!,
                    Group(
                        content = json.encodeToString<GroupContentModel>(
                            GroupContentModel.Text(
                                text
                            )
                        )
                    )
                ) {
                    onUpdated(group.apply { this.group!!.content = it.content })
                    editing = false
                }
                true
            },
            autoFocus = true,
            onDismissRequest = { editing = false },
            showButtons = true
        )
    } else {
        Div({
            style {
                padding(1.r)
            }
            onClick { editing = true }
        }) {
            if (content.text.isNullOrBlank()) {
                // todo: translate
                Empty { Text("No notes.") }
            } else {
                Markdown(content.text!!)
            }
        }
    }
}
