package stories

import Styles
import androidx.compose.runtime.*
import api
import app.AppStyles
import app.ailaai.api.group
import app.dialog.photoDialog
import app.group.GroupInfo
import app.group.GroupItem
import app.widget.ImpactEffortTable
import app.widget.PageTreeWidget
import app.widget.ScriptWidget
import appString
import baseUrl
import com.queatz.db.GroupExtended
import com.queatz.db.StoryContent
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
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
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
    openInNewWindow: Boolean = false
) {
    Style(StoryStyles)
    Style(AppStyles)

    val scope = rememberCoroutineScope()

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

            is StoryContent.Section -> {
                Div({
                    classes(StoryStyles.contentSection)
                }) {
                    Text(part.section)
                }
            }

            is StoryContent.Text -> {
                Div({
                    classes(StoryStyles.contentText)
                }) {
                    LinkifyText(part.text)
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
                                    onSelected = {
                                        onGroupClick(group)
                                    },
                                    info = GroupInfo.LatestMessage
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
                }) {
                    part.photos.forEach { photo ->
                        val url = "$baseUrl$photo"
                        Div({
                            classes(StoryStyles.contentPhotosPhoto)
                            style {
                                backgroundColor(Styles.colors.background)
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
