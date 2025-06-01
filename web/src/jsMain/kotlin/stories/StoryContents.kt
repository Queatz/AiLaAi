package stories

import StyleManager
import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyleSheet
import app.ailaai.api.group
import app.components.FlexInput
import app.dialog.photoDialog
import app.dialog.rememberChoosePhotoDialog
import app.group.GroupInfo
import app.group.GroupItem
import app.widget.FormWidget
import app.widget.ImpactEffortTable
import app.widget.PageTreeWidget
import app.widget.ScriptWidget
import app.widget.SpaceWidget
import app.widget.WebWidget
import app.widget.WidgetStyleSheet
import appString
import application
import baseUrl
import com.queatz.ailaai.api.commentOnStory
import com.queatz.ailaai.api.storyComments
import com.queatz.db.ButtonStyle
import com.queatz.db.Comment
import com.queatz.db.CommentExtended
import com.queatz.db.GroupExtended
import com.queatz.db.InputType
import com.queatz.db.StoryContent
import com.queatz.widgets.Widgets
import components.CardItem
import components.Icon
import components.IconButton
import components.LinkifyText
import components.LoadingText
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import lib.isThisYear
import notBlank
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Source
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Video
import profile.ProfileCard
import r
import time.format
import kotlin.js.Date

@Composable
fun storyStatus(publishDate: Instant?) = publishDate?.let { Date(it.toEpochMilliseconds()) }?.let {
    format(it, "MMMM do${if (isThisYear(it)) "" else ", yyyy"}")
} ?: appString { draft }

@Composable
fun StoryContents(
    content: List<StoryContent>,
    key: Int = 0,
    onGroupClick: (GroupExtended) -> Unit = {},
    onCardClick: ((cardId: String, openInNewWindow: Boolean) -> Unit)? = null,
    onButtonClick: (suspend (script: String, data: String?, input: Map<String, String?>) -> Unit)? = null,
    openInNewWindow: Boolean = false,
    editable: Boolean = false,
    onEdited: ((index: Int, part: StoryContent) -> Unit)? = null,
    onSave: ((List<StoryContent>) -> Unit)? = {},
) {
    StyleManager.use(
        AppStyleSheet::class,
        StoryStyleSheet::class,
        WidgetStyleSheet::class
    )

    val scope = rememberCoroutineScope()

    var input by remember(key, content) {
        mutableStateOf(
            buildMap<String, String?> {
                content.filterIsInstance<StoryContent.Input>().forEach {
                    put(it.key, it.value)
                }
            }
        )
    }

    content.forEachIndexed { index, part ->
        when (part) {
            is StoryContent.Title -> {
                Div({
                    classes(StoryStyles.contentTitle)
                }) {
                    Text(part.title)
                }
            }

            is StoryContent.Authors -> {
                Div({
                    classes(StoryStyles.contentAuthors)
                }) {
                    Span({
                        title("${part.publishDate?.let { Date(it.toEpochMilliseconds()) }}")
                    }) {
                        Text("${storyStatus(part.publishDate)} ${appString { inlineBy }} ")
                        part.authors.forEachIndexed { index, person ->
                            if (index > 0) {
                                Text(", ")
                            }
                            val str = appString { viewProfile }
                            A(href = "/profile/${person.id}", {
                                if (openInNewWindow) {
                                    target(ATarget.Blank)
                                }
                                title(str)
                            }) {
                                Text(person.name ?: appString { someone })
                            }
                        }
                    }
                }
            }

            is StoryContent.Reactions -> {

            }

            is StoryContent.Comments -> {
                val me by application.me.collectAsState()
                var comments by remember(part.story) {
                    mutableStateOf<List<CommentExtended>?>(null)
                }

                suspend fun reloadComments() {
                    api.storyComments(part.story) {
                        comments = it
                    }
                }

                LaunchedEffect(part.story) {
                    reloadComments()
                }

                Div({
                    style {
                        width(100.percent)
                    }
                }) {
                    key(part.story) {
                        var comment by remember {
                            mutableStateOf("")
                        }
                        FlexInput(
                            value = comment,
                            onChange = {
                                comment = it
                            },
                            placeholder = appString { if (me == null) signInToComment else shareAComment },
                            styles = {
                                width(100.percent)
                            },
                            buttonText = appString { post },
                            enabled = me != null,
                            onSubmit = {
                                var success = false
                                api.commentOnStory(
                                    id = part.story,
                                    comment = Comment(comment = comment)
                                ) {
                                    success = true
                                }
                                reloadComments()
                                success
                            }
                        )

                        LoadingText(
                            comments != null,
                            appString { loadingComments }
                        ) {
                            StoryComments(comments!!, max = 2) {
                                reloadComments()
                            }
                        }
                    }
                }
            }

            is StoryContent.Section -> {
                if (editable) {
                    var value by remember(content) { mutableStateOf(part.section) }
                    FlexInput(
                        value = value,
                        onChange = {
                            value = it
                            onEdited?.invoke(index, part.copy(section = it))
                        },
                        singleLine = true,
                        placeholder = appString { section },
                        styles = {
                            margin(0.r)
                            width(100.percent)
                            fontSize(24.px)
                            fontWeight("bold")
                        },
                        onSubmit = {
                            onSave?.invoke(content)
                            true
                        }
                    )
                } else {
                    Div({
                        classes(StoryStyles.contentSection)
                    }) {
                        Text(part.section)
                    }
                }
            }

            is StoryContent.Text -> {
                if (editable) {
                    var value by remember(content) { mutableStateOf(part.text) }
                    FlexInput(
                        value = value,
                        onChange = {
                            value = it
                            onEdited?.invoke(index, part.copy(text = it))
                        },
                        singleLine = true,
                        placeholder = appString { write },
                        styles = {
                            margin(0.r)
                            width(100.percent)
                            fontSize(16.px)
                        },
                        onSubmit = {
                            onSave?.invoke(content)
                            true
                        }
                    )
                } else {
                    Div({
                        classes(StoryStyles.contentText)
                    }) {
                        LinkifyText(part.text)
                    }
                }
            }

            is StoryContent.Groups -> {
                Div({
                    classes(StoryStyles.contentGroups)
                }) {
                    part.groups.forEach { groupId ->
                        var group by remember(groupId) {
                            mutableStateOf<GroupExtended?>(null)
                        }

                        LaunchedEffect(groupId) {
                            api.group(groupId) {
                                group = it
                            }
                        }

                        LoadingText(group != null, appString { loadingGroup }) {
                            group?.let { group ->
                                GroupItem(
                                    group,
                                    selectable = true,
                                    selected = false,
                                    shadow = true,
                                    onSelected = {
                                        onGroupClick(group)
                                    },
                                    info = GroupInfo.Members,
                                    coverPhoto = part.coverPhotos
                                )
                            }
                        }
                    }
                }
            }

            is StoryContent.Cards -> {
                Div({
                    classes(StoryStyles.contentCards)
                }) {
                    part.cards.forEach { card ->
                        CardItem(
                            cardId = card,
                            onClick = onCardClick?.let { onCardClick ->
                                { openInNewWindow ->
                                    onCardClick(card, openInNewWindow)
                                }
                            },
                        )
                    }
                }
            }

            is StoryContent.Photos -> {
                Div({
                    classes(StoryStyles.contentPhotos)

                    if (part.photos.size > 1) {
                        classes(StoryStyles.contentPhotosMulti)
                    }
                }) {
                    part.photos.forEach { photo ->
                        val url = "$baseUrl$photo"
                        if (part.aspect == null) {
                            Img(url) {
                                classes(StoryStyles.contentPhotosPhotoNoAspect)

                                onClick {
                                    scope.launch {
                                        photoDialog(url)
                                    }
                                }
                            }
                        } else {
                            Div({
                                classes(StoryStyles.contentPhotosPhoto)

                                style {
                                    backgroundImage("url($url)")
                                    property("aspect-ratio", "${part.aspect}")
                                }

                                onClick {
                                    scope.launch {
                                        photoDialog(url)
                                    }
                                }
                            })
                        }
                    }
                }
            }

            is StoryContent.Audio -> {
                key(part.audio) {
                    Audio({
                        classes(StoryStyles.contentAudio)
                        attr("controls", "")
                        style {
                            width(100.percent)
                        }
                    }) {
                        Source({
                            attr("src", "$baseUrl${part.audio}")
                            attr("type", "audio/mp4")
                        })
                    }
                }
            }

            is StoryContent.Video -> {
                Div({
                    style {
                        width(100.percent)
                        margin(1.r, 0.r)
                    }
                }) {
                    Video({
                        classes(Styles.video)

                        attr("controls", "")
                        attr("preload", "metadata")
                        attr("loop", "")
                        attr("playsinline", "")
                    }) {
                        Source({
                            attr("src", "$baseUrl${part.video}")
                            attr("type", "video/mp4")
                        })
                    }
                }
            }

            is StoryContent.Divider -> {
                Div({
                    classes(StoryStyles.divider)
                }) {
                    Icon("flare")
                }
            }

            is StoryContent.Widget -> {
                when (part.widget) {
                    Widgets.ImpactEffortTable -> {
                        ImpactEffortTable(part.id)
                    }

                    Widgets.PageTree -> {
                        PageTreeWidget(part.id)
                    }

                    Widgets.Script -> {
                        ScriptWidget(part.id)
                    }

                    Widgets.Web -> {
                        WebWidget(part.id)
                    }

                    Widgets.Form -> {
                        FormWidget(part.id)
                    }

                    Widgets.Space -> {
                        SpaceWidget(part.id)
                    }

                    Widgets.Shop -> {
                        // todo
                    }
                }
            }

            is StoryContent.Button -> {
                var isDisabled by remember(part) { mutableStateOf(false) }
                val buttonEnabled = part.enabled != false // null or true means enabled

                Button({
                    classes(
                        if (part.style == ButtonStyle.Secondary) {
                            Styles.outlineButton
                        } else {
                            Styles.button
                        }
                    )

                    if (isDisabled || !buttonEnabled) {
                        disabled()
                    }

                    part.color?.let {
                        style {
                            if (part.style == ButtonStyle.Secondary) {
                                border(1.px, LineStyle.Solid, Color(it))
                                color(Color(it))
                            } else {
                                backgroundColor(Color(it))
                            }
                        }
                    }

                    onClick {
                        if (!isDisabled && buttonEnabled) {
                            isDisabled = true
                            scope.launch {
                                onButtonClick?.invoke(part.script, part.data, input)
                                isDisabled = false
                            }
                        }
                    }
                }) {
                    Text(part.text)
                }
            }

            is StoryContent.Input -> {
                var value by remember(key, part) {
                    mutableStateOf(part.value.orEmpty())
                }

                when (part.inputType) {
                    InputType.Text -> {
                        FlexInput(
                            value = value,
                            onChange = {
                                input = input + (part.key to it)
                                value = it
                            },
                            styles = {
                                width(100.percent)
                            },
                            placeholder = part.hint.orEmpty()
                        )
                    }

                    InputType.Photo -> {
                        val choosePhoto = rememberChoosePhotoDialog(showUpload = true)
                        val isGenerating = choosePhoto.isGenerating.collectAsState().value

                        if (value.isBlank()) {
                            Button({
                                classes(Styles.outlineButton)
                                if (isGenerating) {
                                    disabled()
                                }
                                onClick {
                                    scope.launch {
                                        choosePhoto.launch { photoUrl, _, _ ->
                                            input = input + (part.key to photoUrl)
                                            value = photoUrl
                                        }
                                    }
                                }
                            }) {
                                // todo: translate
                                Text(part.hint?.notBlank ?: "Choose photo")
                            }
                        } else {
                            Div({
                                style {
                                    position(Position.Relative)
                                    width(100.percent)
                                }
                            }) {
                                val src = "$baseUrl$value"
                                Img(src = src) {
                                    style {
                                        width(100.percent)
                                        borderRadius(1.r)
                                        overflow("hidden")
                                    }
                                    onClick {
                                        scope.launch {
                                            photoDialog(src)
                                        }
                                    }
                                }

                                // Delete button
                                IconButton(
                                    name = "delete",
                                    title = appString { remove },
                                    background = true,
                                    styles = {
                                        position(Position.Absolute)
                                        top(1.r)
                                        right(1.r)
                                        color(Styles.colors.red)
                                    }
                                ) {
                                    input = input + (part.key to "")
                                    value = ""
                                }
                            }
                        }
                    }
                }
            }

            is StoryContent.Profiles -> {
                Div({
                    classes(StoryStyles.contentProfiles)
                }) {
                    part.profiles.forEach { personId ->
                        ProfileCard(personId) {
                            window.open("/profile/$personId", "_blank")
                        }
                    }
                }
            }
        }
    }
}
