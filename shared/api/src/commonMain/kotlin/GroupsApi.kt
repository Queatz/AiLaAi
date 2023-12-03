package app.ailaai.api

import com.queatz.db.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

suspend fun Api.messages(
    group: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Message>>,
) = get("groups/$group/messages", onError = onError, onSuccess = onSuccess)

suspend fun Api.groupCards(
    group: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get("groups/$group/cards", onError = onError, onSuccess = onSuccess)


suspend fun Api.messagesBefore(
    group: String,
    before: Instant,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Message>>,
) = get(
    url = "groups/$group/messages",
    parameters = mapOf(
        "before" to before.toString()
    ),
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.groups(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GroupExtended>>,
) = get("groups", onError = onError, onSuccess = onSuccess)

suspend fun Api.exploreGroups(
    geo: Geo,
    search: String? = null,
    public: Boolean = false,
    onError: ErrorBlock = null,
    offset: Int = 0,
    limit: Int = 100,
    onSuccess: SuccessBlock<List<GroupExtended>>,
) = get(
    url = "groups/explore",
    parameters = mapOf(
        "geo" to geo.toString(),
        "search" to search,
        "public" to public.toString(),
        "limit" to limit.toString(),
        "offset" to offset.toString(),
    ),
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.group(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GroupExtended>,
) = get("groups/$id", onError = onError, onSuccess = onSuccess)

suspend fun Api.createGroup(
    people: List<String>,
    reuse: Boolean = false,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Group>,
) =
    post("groups", CreateGroupBody(people.toSet().toList(), reuse), onError = onError, onSuccess = onSuccess)

suspend fun Api.groupsWith(
    people: List<String>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GroupExtended>>,
) =
    post("groups/search", SearchGroupBody(people.toSet().toList()), onError = onError, onSuccess = onSuccess)

suspend fun Api.updateGroup(
    id: String,
    groupUpdate: Group,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Group> = {},
) = post("groups/$id", groupUpdate, onError = onError, onSuccess = onSuccess)

suspend fun Api.sendMedia(
    group: String,
    photos: List<ByteArray>,
    message: Message? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return post("groups/$group/photos", MultiPartFormDataContent(
        formData {
            if (message != null) {
                append("message", httpJson.encodeToString(message))
            }
            photos.forEachIndexed { index, photo ->
                append(
                    "photo[$index]",
                    photo,
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
                    }
                )
            }
        }
    ), client = httpDataClient, onError = onError, onSuccess = onSuccess)
}

suspend fun Api.sendAudio(
    group: String,
    audio: InputProvider,
    message: Message? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) {
    return post("groups/$group/audio", MultiPartFormDataContent(
        formData {
            if (message != null) {
                append("message", httpJson.encodeToString(message))
            }
            append(
                "audio",
                audio,
                Headers.build {
                    append(HttpHeaders.ContentType, "audio/mp4")
                    append(HttpHeaders.ContentDisposition, "filename=audio.m4a")
                }
            )
        }
    ), client = httpDataClient, onError = onError, onSuccess = onSuccess)
}

suspend fun Api.sendVideos(
    group: String,
    videos: List<Pair<InputProvider, String>>,
    message: Message? = null,
    uploadCallback: (Float) -> Unit,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return post(
        "groups/$group/videos",
        MultiPartFormDataContent(
            formData {
                if (message != null) {
                    append("message", httpJson.encodeToString(message))
                }
                videos.forEachIndexed { index, video ->
                    append(
                        "photo[$index]",
                        video.first,
                        Headers.build {
                            append(HttpHeaders.ContentType, video.second)
                            append(HttpHeaders.ContentDisposition, "filename=${video.second.split("/").lastOrNull() ?: ""}")
                        }
                    )
                }
            }
        ),
        progressCallback = uploadCallback,
        client = httpDataClient,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.sendMessage(
    group: String,
    message: Message,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("groups/$group/messages", message, onError = onError, onSuccess = onSuccess)
