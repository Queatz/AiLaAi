package app.ailaai.api

import com.queatz.db.Message
import com.queatz.db.Rating
import com.queatz.db.ReactBody
import com.queatz.db.ReactionAndPerson
import io.ktor.http.HttpStatusCode

suspend fun Api.message(
    message: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Message>,
) = get(
    "messages/$message",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateMessage(
    id: String,
    messageUpdate: Message,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    "messages/$id",
    messageUpdate,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.reactToMessage(
    id: String,
    react: ReactBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("messages/$id/react", react, onError = onError, onSuccess = onSuccess)

suspend fun Api.messageReactions(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<ReactionAndPerson>> = {}
) = get("messages/$id/reactions", onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteMessage(
    message: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    "messages/$message/delete",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.messageRating(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Rating>,
) = get(
    url = "/messages/$id/rating",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.setMessageRating(
    id: String,
    rating: Rating,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Rating> = {}
) = post(
    url = "/messages/$id/rating",
    body = rating,
    onError = onError,
    onSuccess = onSuccess
)
