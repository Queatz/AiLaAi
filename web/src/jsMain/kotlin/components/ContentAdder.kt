package components

import LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.newCard
import app.ailaai.api.uploadAudio
import app.ailaai.api.uploadVideo
import app.dialog.dialog
import app.dialog.editFormDialog
import app.dialog.inputDialog
import app.dialog.rememberChoosePhotoDialog
import app.dialog.selectCardDialog
import app.dialog.selectGroupDialog
import app.dialog.selectPersonDialog
import app.menu.InlineMenu
import app.menu.Menu
import appString
import createWidget
import com.queatz.db.StoryContent
import com.queatz.widgets.Widgets
import app.dialog.selectScriptDialog
import app.dialog.selectSceneDialog
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormOptions
import com.queatz.widgets.widgets.PageTreeData
import com.queatz.widgets.widgets.SpaceData
import com.queatz.widgets.widgets.WebData
import json
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.alignSelf
import org.w3c.dom.HTMLElement
import org.w3c.dom.DOMRect
import pickAudio
import pickVideo
import toBytes

enum class ContentType {
    Section,
    Text,
    Photos,
    Audio,
    Video,
    Cards,
    Groups,
    Profiles,
    Widget,
    Scene
}

@Composable
fun ContentAdder(
    onContentAdded: (StoryContent) -> Unit,
    availableTypes: Set<ContentType> = ContentType.entries.toSet(),
    buttonText: String = "Add",
    buttonIcon: String = "add",
    cardId: String? = null // For card-specific widgets
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    var menuTarget by remember { mutableStateOf<DOMRect?>(null) }
    val choosePhoto = rememberChoosePhotoDialog(showUpload = true)

    // Suspend function to ensure we have a card ID - creates one if needed
    suspend fun ensureCardId(): String {
        // Use existing ID if we have one
        cardId?.let { return it }

        // Otherwise create a new card
        var newId = ""
        api.newCard { newCard ->
            newId = newCard.id!!
        }
        return newId
    }

    IconButton(
        name = buttonIcon,
        title = buttonText,
        styles = {
            alignSelf(AlignSelf.Center)
        }
    ) {
        menuTarget = if (menuTarget == null) {
            (it.target as HTMLElement).getBoundingClientRect()
        } else {
            null
        }
    }

    if (menuTarget != null) {
        Menu({ menuTarget = null }, menuTarget!!) {
            if (ContentType.Section in availableTypes) {
                item(appString { section }, icon = "title") {
                    scope.launch {
                        val sectionText = inputDialog(
                            title = "Section",
                            placeholder = "",
                            confirmButton = "Add",
                            defaultValue = ""
                        )

                        if (sectionText != null) {
                            onContentAdded(StoryContent.Section(sectionText))
                        }
                    }
                }
            }

            if (ContentType.Text in availableTypes) {
                item(appString { text }, icon = "notes") {
                    scope.launch {
                        val textContent = inputDialog(
                            title = "Text",
                            placeholder = "",
                            confirmButton = "Add",
                            defaultValue = ""
                        )

                        if (textContent != null) {
                            onContentAdded(StoryContent.Text(textContent))
                        }
                    }
                }
            }

            if (ContentType.Photos in availableTypes) {
                item(appString { photo }, icon = "photo") {
                    choosePhoto.launch(multiple = true) { photoUrl, _, _ ->
                        onContentAdded(StoryContent.Photos(photos = listOf(photoUrl)))
                    }
                }
            }

            if (ContentType.Audio in availableTypes) {
                item(appString { audio }, icon = "audiotrack") {
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
            }

            if (ContentType.Video in availableTypes) {
                item(appString { video }, icon = "videocam") {
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
            }

            if (ContentType.Cards in availableTypes) {
                item(appString { card }, icon = "style") {
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
            }

            if (ContentType.Groups in availableTypes) {
                item(appString { group }, icon = "group") {
                    scope.launch {
                        val selected = selectGroupDialog(
                            configuration = configuration
                        )
                        selected?.group?.id?.let { groupId ->
                            onContentAdded(StoryContent.Groups(groups = listOf(groupId)))
                        }
                    }
                }
            }

            if (ContentType.Profiles in availableTypes) {
                item(appString { profile }, icon = "person") {
                    scope.launch {
                        selectPersonDialog(
                            configuration = configuration
                        ) { person ->
                            person.id?.let { personId ->
                                onContentAdded(StoryContent.Profiles(profiles = listOf(personId)))
                            }
                        }
                    }
                }
            }

            if (ContentType.Scene in availableTypes) {
                item("Scene", icon = "landscape") {
                    scope.launch {
                        selectSceneDialog(scope) { sceneId ->
                            onContentAdded(StoryContent.Scene(sceneId))
                        }
                    }
                }
            }

            if (ContentType.Widget in availableTypes) {
                item("Widget", icon = "widgets") {
                    scope.launch {
                        dialog("", cancelButton = null) {
                            InlineMenu({
                                it(true)
                            }) {
                                item(
                                    "Script",
                                    description = "Embed dynamic content into your page",
                                    icon = "code"
                                ) {
                                    scope.launch {
                                        selectScriptDialog(scope) { scriptId, scriptData ->
                                            scope.launch {
                                                api.createWidget(
                                                    widget = Widgets.Script,
                                                    data = scriptData
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

                                item(
                                    "Page Tree",
                                    description = "Organize small projects and track progress",
                                    icon = "account_tree"
                                ) {
                                    scope.launch {
                                        // Ensure we have a card ID before creating the widget
                                        val cid = ensureCardId()
                                        api.createWidget(
                                            widget = Widgets.PageTree,
                                            data = json.encodeToString(PageTreeData(card = cid))
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

                                item("Space", "Create a sketch or presentation", icon = "space_dashboard") {
                                    scope.launch {
                                        // Ensure we have a card ID before creating the widget
                                        val cid = ensureCardId()
                                        api.createWidget(
                                            widget = Widgets.Space,
                                            data = json.encodeToString(SpaceData(card = cid))
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

                                item("Website", "Embed a URL", icon = "language") {
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
                                    "Form",
                                    description = "Collect and organize responses to a form",
                                    icon = "list_alt"
                                ) {
                                    scope.launch {
                                        // Ensure we have a card ID before creating the form
                                        val cid = ensureCardId()
                                        val result = editFormDialog(
                                            initialFormData = FormData(
                                                page = cid,
                                                options = FormOptions(enableAnonymousReplies = true)
                                            )
                                        ) { formData ->
                                            scope.launch {
                                                api.createWidget(
                                                    widget = Widgets.Form,
                                                    data = json.encodeToString(formData)
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
                            }
                        }
                    }
                }
            }
        }
    }
}
