package stories

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.group
import app.components.EditField
import app.components.TextBox
import app.dialog.photoDialog
import app.group.GroupInfo
import app.group.GroupItem
import app.widget.ImpactEffortTable
import app.widget.PageTreeWidget
import app.widget.ScriptWidget
import app.widget.WebWidget
import appString
import application
import baseUrl
import com.queatz.ailaai.api.commentOnStory
import com.queatz.ailaai.api.reactToStory
import com.queatz.ailaai.api.storyComments
import com.queatz.db.*
import com.queatz.widgets.Widgets
import components.CardItem
import components.Icon
import components.LinkifyText
import components.LoadingText
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import lib.format
import lib.isThisYear
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Source
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

@Composable
fun storyStatus(publishDate: Instant?) = publishDate?.let { Date(it.toEpochMilliseconds()) }?.let {
    format(it, "MMMM do${if (isThisYear(it)) "" else ", yyyy"}")
} ?: appString { draft }

@Composable
fun StoryContents(
    storyContent: List<StoryContent>,
    onGroupClick: (GroupExtended) -> Unit,
    onButtonClick: ((script: String, data: String?) -> Unit)? = null,
    openInNewWindow: Boolean = false,
    editable: Boolean = false,
    onEdited: (() -> Unit)? = null,
    onSave: ((List<StoryContent>) -> Unit)? = {}
) {
    Style(StoryStyles)
    Style(AppStyles)

    val scope = rememberCoroutineScope()
    val currentRecomposeScope = currentRecomposeScope

    storyContent.forEach { part ->
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
                ReactionItem(
                    reactions = part.reactions!!.all,
                    onAddReaction = { reaction ->
                        scope.launch {
                            api.reactToStory(
                                part.story,
                                ReactBody(Reaction(reaction = reaction))
                            )
                        }
                    },
                    onReactionComment = {reaction, comment ->
                        scope.launch {
                            api.reactToStory(
                                part.story,
                                ReactBody(Reaction(reaction = reaction, comment = comment))
                            )
                        }
                    }
                )
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
                        EditField(
                            placeholder = appString { if (me == null) signInToComment else shareAComment },
                            styles = {
                                width(100.percent)
                            },
                            buttonBarStyles = {
                                width(100.percent)
                                justifyContent(JustifyContent.End)
                            },
                            showDiscard = false,
                            resetOnSubmit = true,
                            enabled = me != null,
                            button = appString { post }
                        ) {
                            var success = false
                            api.commentOnStory(
                                part.story,
                                Comment(comment = it)
                            ) {
                                success = true
                            }
                            reloadComments()
                            success
                        }

                        LoadingText(
                            comments != null,
                            appString { loadingComments }
                        ) {
                            StoryComments(comments!!, max = 3) {
                                reloadComments()
                            }
                        }
                    }
                }
            }

            is StoryContent.Section -> {
                if (editable) {
                    TextBox(
                        part.section,
                        onValue = { part.section = it; currentRecomposeScope.invalidate(); onEdited?.invoke() },
                        inline = true,
                        // todo translate
                        placeholder = "Section",
                        styles = {
                            margin(0.r)
                            width(100.percent)
                            fontSize(24.px)
                            fontWeight("bold")
                        }
                    ) {
                        onSave?.invoke(storyContent)
                    }
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
                    TextBox(
                        part.text,
                        onValue = { part.text = it; currentRecomposeScope.invalidate(); onEdited?.invoke() },
                        inline = true,
                        // todo translate
                        placeholder = "Write",
                        styles = {
                            margin(0.r)
                            width(100.percent)
                            fontSize(16.px)
                        }
                    ) {
                        onSave?.invoke(storyContent)
                    }
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
                                    info = GroupInfo.LatestMessage,
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
                        CardItem(card)
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
                }
            }

            is StoryContent.Button -> {
                Div({
                    classes(Styles.button)

                    onClick {
                        onButtonClick?.invoke(part.script, part.data)
                    }
                }) {
                    Text(part.text)
                }
            }
        }
    }
}
