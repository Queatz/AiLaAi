package app.page

import LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import api
import createWidget
import app.dialog.inputDialog
import app.dialog.rememberChoosePhotoDialog
import app.dialog.selectCardDialog
import app.dialog.selectGroupDialog
import app.dialog.selectPersonDialog
import app.ailaai.api.uploadAudio
import app.ailaai.api.uploadVideo
import app.menu.Menu
import appString
import com.queatz.db.StoryContent
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.ImpactEffortTableData
import com.queatz.widgets.widgets.PageTreeData
import com.queatz.widgets.widgets.SpaceData
import com.queatz.widgets.widgets.WebData
import components.IconButton
import json
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import pickAudio
import pickVideo
import r
import toBytes

@Composable
fun StoryCreatorToolbar(
    onContentAdded: (StoryContent) -> Unit
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val choosePhoto = rememberChoosePhotoDialog(showUpload = true)
    var widgetMenuTarget by remember { mutableStateOf<DOMRect?>(null) }

    widgetMenuTarget?.let { target ->
        Menu(
            onDismissRequest = { widgetMenuTarget = null },
            target = target
        ) {
            item(
                title = "Script",
                icon = "code"
            ) {
                scope.launch {
                    api.createWidget(
                        widget = Widgets.Script
                    ) { widget ->
                        onContentAdded(
                            StoryContent.Widget(
                                widget = widget.widget!!,
                                id = widget.id!!
                            )
                        )
                    }
                }
            }
            item(
                title = "Space",
                icon = "space_dashboard"
            ) {
                scope.launch {
                    api.createWidget(
                        widget = Widgets.Space,
                        data = json.encodeToString(SpaceData())
                    ) { widget ->
                        onContentAdded(
                            StoryContent.Widget(
                                widget = widget.widget!!,
                                id = widget.id!!
                            )
                        )
                    }
                }
            }
            item(
                title = "Website",
                icon = "language"
            ) {
                scope.launch {
                    val url = inputDialog(
                        title = "Website URL",
                        placeholder = "https://",
                        confirmButton = "Add",
                        defaultValue = ""
                    )
                    if (url != null) {
                        api.createWidget(
                            widget = Widgets.Web,
                            data = json.encodeToString(WebData(url = url))
                        ) { widget ->
                            onContentAdded(
                                StoryContent.Widget(
                                    widget = widget.widget!!,
                                    id = widget.id!!
                                )
                            )
                        }
                    }
                }
            }
            item(
                title = "Impact/Effort Table",
                icon = "grid_on"
            ) {
                scope.launch {
                    api.createWidget(
                        widget = Widgets.ImpactEffortTable,
                        data = json.encodeToString(ImpactEffortTableData())
                    ) { widget ->
                        onContentAdded(
                            StoryContent.Widget(
                                widget = widget.widget!!,
                                id = widget.id!!
                            )
                        )
                    }
                }
            }
            item(
                title = "Page Tree",
                icon = "account_tree"
            ) {
                scope.launch {
                    api.createWidget(
                        widget = Widgets.PageTree,
                        data = json.encodeToString(PageTreeData())
                    ) { widget ->
                        onContentAdded(
                            StoryContent.Widget(
                                widget = widget.widget!!,
                                id = widget.id!!
                            )
                        )
                    }
                }
            }
        }
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexWrap(FlexWrap.Wrap)
            alignItems(AlignItems.Center)
            gap(.5.r)
            padding(.5.r, 1.r)
        }
    }) {
        IconButton(
            name = "title",
            title = appString { section },
            text = appString { section }
        ) {
            onContentAdded(StoryContent.Section(section = ""))
        }

        IconButton(
            name = "notes",
            title = appString { text },
            text = appString { text }
        ) {
            onContentAdded(StoryContent.Text(text = ""))
        }

        IconButton(
            name = "photo",
            title = appString { photo },
            text = appString { photo }
        ) {
            choosePhoto.launch(multiple = true) { photoUrl, _, _ ->
                onContentAdded(StoryContent.Photos(photos = listOf(photoUrl)))
            }
        }

        IconButton(
            name = "audiotrack",
            title = appString { audio },
            text = appString { audio }
        ) {
            pickAudio { file ->
                scope.launch {
                    api.uploadAudio(
                        audio = file.toBytes(),
                        contentType = file.type.ifBlank { "audio/mp4" },
                        filename = file.name.ifBlank { "audio.m4a" }
                    ) { response ->
                        onContentAdded(StoryContent.Audio(audio = response.urls.first()))
                    }
                }
            }
        }

        IconButton(
            name = "videocam",
            title = appString { video },
            text = appString { video }
        ) {
            pickVideo { file ->
                scope.launch {
                    api.uploadVideo(
                        video = file.toBytes(),
                        contentType = file.type.ifBlank { "video/mp4" },
                        filename = file.name.ifBlank { "video.mp4" }
                    ) { response ->
                        onContentAdded(StoryContent.Video(video = response.urls.first()))
                    }
                }
            }
        }

        IconButton(
            name = "style",
            title = appString { card },
            text = appString { card }
        ) {
            scope.launch {
                selectCardDialog(
                    configuration = configuration,
                    onSelected = { selected ->
                        selected?.id?.let { cardId ->
                            onContentAdded(StoryContent.Cards(cards = listOf(cardId)))
                        }
                    }
                )
            }
        }

        IconButton(
            name = "group",
            title = appString { group },
            text = appString { group }
        ) {
            scope.launch {
                val selected = selectGroupDialog(configuration = configuration)
                selected?.group?.id?.let { groupId ->
                    onContentAdded(StoryContent.Groups(groups = listOf(groupId)))
                }
            }
        }

        IconButton(
            name = "person",
            title = appString { profile },
            text = appString { profile }
        ) {
            scope.launch {
                selectPersonDialog(configuration = configuration) { person ->
                    person.id?.let { personId ->
                        onContentAdded(StoryContent.Profiles(profiles = listOf(personId)))
                    }
                }
            }
        }

        IconButton(
            name = "widgets",
            title = "Widget",
            text = "Widget"
        ) { event: SyntheticMouseEvent ->
            widgetMenuTarget = if (widgetMenuTarget == null) {
                (event.target as? HTMLElement)?.getBoundingClientRect()
            } else {
                null
            }
        }
    }
}
