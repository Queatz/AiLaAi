package stories

import StyleManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.comment
import app.dialog.dialog
import app.messaages.inList
import application
import com.queatz.db.CommentExtended
import components.Loading
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import r

suspend fun commentDialog(comment: CommentExtended) = dialog(
    title = application.appString { commentReplies },
    confirmButton = application.appString { close },
    maxWidth = 800.px,
    cancelButton = null
) {
    StyleManager.use(
        StoryStyleSheet::class
    )

    var comment by remember { mutableStateOf(comment) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun reloadCommentReplies() {
        api.comment(comment.comment!!.id!!) {
            comment = it
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        reloadCommentReplies()
    }

    StoryComments(
        comment.inList(),
        showRepliesLink = !isLoading,
        loadRepliesInline = true
    ) {
        reloadCommentReplies()
    }
    if (isLoading) {
        Loading {
            style {
                padding(2.r)
            }
        }
    }
}
