package app.ailaai.api

import com.queatz.db.Comment
import com.queatz.db.CommentExtended

suspend fun Api.replyToComment(
    id: String,
    comment: Comment,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Comment> = {}
) = post("comments/$id/reply", comment, onError = onError, onSuccess = onSuccess)

suspend fun Api.comment(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<CommentExtended> = {}
) = get("comments/$id", onError = onError, onSuccess = onSuccess)
