package app.group

import LocalConfiguration
import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import api
import app.ailaai.api.updateGroup
import app.dialog.selectCardDialog
import app.dialog.selectScriptDialog
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import json
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import r
import com.queatz.db.GroupContent as GroupContentModel

@Composable
fun GroupContent(
    group: GroupExtended,
    onUpdated: (GroupExtended) -> Unit,
    setTitle: (String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val content = group.group?.content?.let {
        try {
            json.decodeFromString<GroupContentModel>(it)
        } catch (e: Exception) {
            null
        }
    }

    if (content == null || content is GroupContentModel.None) {
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                alignItems(AlignItems.Stretch)
                justifyContent(JustifyContent.Center)
                flex(1)
                overflow("auto")
                padding(1.r)
                gap(1.r)
            }
        }) {
            FeatureButton(
                icon = "edit",
                title = "Notes",
                description = "Add simple notes"
            ) {
                scope.launch {
                    api.updateGroup(
                        group.group!!.id!!,
                        Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Text("")))
                    ) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
            FeatureButton(
                icon = "check_box",
                title = "Tasks",
                description = "Show the group's tasks"
            ) {
                scope.launch {
                    api.updateGroup(
                        group.group!!.id!!,
                        Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Tasks()))
                    ) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
            FeatureButton(
                icon = "description",
                title = "Page",
                description = "Show a page"
            ) {
                scope.launch {
                    selectCardDialog(configuration) { card ->
                        if (card != null) {
                            scope.launch {
                                api.updateGroup(
                                    group.group!!.id!!,
                                    Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Card(card.id)))
                                ) {
                                    onUpdated(group.apply { this.group!!.content = it.content })
                                }
                            }
                        }
                    }
                }
            }
            FeatureButton(
                icon = "code",
                title = "Script",
                description = "Show a script"
            ) {
                scope.launch {
                    selectScriptDialog(scope) { scriptId, scriptData ->
                        scope.launch {
                            api.updateGroup(
                                group.group!!.id!!,
                                Group(
                                    content = json.encodeToString<GroupContentModel>(
                                        GroupContentModel.Script(
                                            scriptId,
                                            scriptData
                                        )
                                    )
                                )
                            ) {
                                onUpdated(group.apply { this.group!!.content = it.content })
                            }
                        }
                    }
                }
            }
            FeatureButton(
                icon = "public",
                title = "Website",
                description = "Show a website"
            ) {
                scope.launch {
                    api.updateGroup(
                        group.group!!.id!!,
                        Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Website("")))
                    ) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
        }
    } else {
        key(group.group?.id) {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    height(0.r)
                    flex(1)
                    overflow("auto")
                }
            }) {
                when (content) {
                    is GroupContentModel.Text -> GroupContentText(group, content, onUpdated)
                    is GroupContentModel.Tasks -> GroupContentTasks(group)
                    is GroupContentModel.Card -> GroupContentCard(content, setTitle)
                    is GroupContentModel.Script -> GroupContentScript(content, setTitle)
                    is GroupContentModel.Website -> GroupContentWebsite(group, content, onUpdated, setTitle)
                    else -> Unit
                }
            }
        }
    }
}

private fun String.ensureScheme() = if (contains("://")) this else "https://$this"
