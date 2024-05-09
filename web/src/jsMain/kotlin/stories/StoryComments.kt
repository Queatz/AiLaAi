package stories

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.comment
import app.ailaai.api.replyToComment
import app.components.EditField
import appString
import appText
import application
import com.queatz.db.Comment
import com.queatz.db.CommentExtended
import components.LinkifyText
import components.ProfilePhoto
import format
import kotlinx.browser.window
import kotlinx.coroutines.launch
import lib.formatDistanceToNow
import lib.toLocaleString
import notEmpty
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

@Composable
fun StoryComments(
    comments: List<CommentExtended>,
    showRepliesLink: Boolean = true,
    loadRepliesInline: Boolean = false,
    max: Int? = null,
    onReply: suspend (Comment) -> Unit
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    var showAll by remember { mutableStateOf(max == null) }

    var loadedCommentReplies by remember {
        mutableStateOf(emptyMap<String, List<CommentExtended>>())
    }

    suspend fun loadCommentReplies(comment: CommentExtended) {
        api.comment(comment.comment!!.id!!) {
            loadedCommentReplies = loadedCommentReplies + (it.comment!!.id!! to it.replies!!)
        }
    }

    fun openComment(comment: CommentExtended) {
        scope.launch {
            commentDialog(comment)
        }
    }

    Div({
        classes(StoryStyles.comments)
    }) {
        val maxShown = max != null && comments.size > max && !showAll

        if (maxShown) {
            comments.take(max!!)
        } else {
            comments
        }.forEach { comment ->
            key(comment.comment!!.id!!) {
                var showReply by remember {
                    mutableStateOf(false)
                }

                Div({
                    classes(StoryStyles.comment)
                }) {
                    ProfilePhoto(comment.person!!, onClick = {
                        window.open("/profile/${comment.person!!.id!!}", "_blank")
                    })
                    Div({
                        classes(StoryStyles.commentLayout)
                    }) {
                        Div({
                            classes(StoryStyles.commentBox)
                        }) {
                            Div({
                                classes(StoryStyles.commentInfo)
                            }) {
                                Text(comment.person!!.name ?: appString { someone })
                            }
                            Div({
                                classes(StoryStyles.commentComment)
                            }) {
                                LinkifyText(comment.comment?.comment ?: appString { someone })
                            }
                        }
                        Div({
                            classes(StoryStyles.commentTime)
                        }) {
                            Div({
                                style {
                                    opacity(.5f)
                                    marginRight(.5.r)
                                }

                                title(Date(comment.comment!!.createdAt!!.toEpochMilliseconds()).toString())
                            }) {
                                Text(
                                    formatDistanceToNow(
                                        Date(comment.comment!!.createdAt!!.toEpochMilliseconds()),
                                        js("{ addSuffix: true }")
                                    )
                                )
                            }
                            Div({
                                classes(Styles.inlineButton)

                                onClick {
                                    showReply = !showReply
                                }
                            }) {
                                appText { reply }
                            }
                        }

                        val showTotalReplies = comment.totalReplies!! > 0
                        val replies = (comment.replies ?: loadedCommentReplies[comment.comment!!.id!!])?.notEmpty

                        if (showReply || replies != null || showTotalReplies) {
                            Div({
                                classes(StoryStyles.commentRepliesLayout)
                            }) {
                                if (showReply) {
                                    EditField(
                                        placeholder = if (me == null) appString {
                                            signInToReply
                                        } else "${appString { replyTo }} ${comment.person!!.name ?: appString { someone }}",
                                        styles = {
                                            marginTop(.5.r)
                                            width(100.percent)
                                        },
                                        buttonBarStyles = {
                                            justifyContent(JustifyContent.End)
                                            width(100.percent)
                                        },
                                        autoFocus = true,
                                        showDiscard = false,
                                        resetOnSubmit = true,
                                        enabled = me != null,
                                        button = appString { post }
                                    ) {
                                        var success = false
                                        api.replyToComment(
                                            comment.comment!!.id!!,
                                            Comment(comment = it)
                                        ) {
                                            success = true
                                            if (loadRepliesInline) {
                                                loadCommentReplies(comment)
                                            }
                                            onReply(it)
                                            showReply = false
                                        }
                                        success
                                    }
                                }

                                replies?.let { replies ->
                                    StoryComments(
                                        replies,
                                        max = max,
                                        loadRepliesInline = loadRepliesInline,
                                        onReply = onReply
                                    )
                                } ?: let {
                                    if (showRepliesLink && showTotalReplies) {
                                        Div({
                                            style {
                                                marginLeft(.75.r)
                                            }
                                        }) {
                                            Span({
                                                style {
                                                    opacity(.5f)
                                                }
                                            }) { Text("⮡ ") }
                                            Span({
                                                classes(Styles.inlineButton)

                                                onClick {
                                                    if (loadRepliesInline != it.ctrlKey) {
                                                        scope.launch {
                                                            loadCommentReplies(comment)
                                                        }
                                                    } else {
                                                        openComment(comment)
                                                    }
                                                }
                                            }) {
                                                Text("${comment.totalReplies} ${appString { if (comment.totalReplies == 1) inlineReply else inlineReplies }}")
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

        if (maxShown) {
            Button({
                classes(Styles.outlineButton)

                style {
                    alignSelf(AlignSelf.FlexStart)
                    height(2.5.r)
                }

                onClick {
                    showAll = true
                }
            }) {
                val remaining = comments.size - max!!
                Text(appString { if (remaining == 1) showAllComment else showAllComments }.format(
                    remaining.toLocaleString()
                ))
            }
        }
    }
}
