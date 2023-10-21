package com.queatz.ailaai.api

import android.net.Uri
import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.SuccessBlock
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg
import com.queatz.ailaai.extensions.asScaledVideo
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
private data class CreateGroupBody(val people: List<String>, val reuse: Boolean)

suspend fun Api.messages(
    group: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Message>>,
) = get("groups/$group/messages", onError = onError, onSuccess = onSuccess)

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
    geo: List<Double>,
    search: String? = null,
    public: Boolean = false,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GroupExtended>>,
) = get(
    url = "groups/explore",
    parameters = mapOf(
        "geo" to geo.joinToString(","),
        "search" to search,
        "public" to public.toString()
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

suspend fun Api.updateGroup(
    id: String,
    groupUpdate: Group,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Group> = {},
) = post("groups/$id", groupUpdate, onError = onError, onSuccess = onSuccess)

suspend fun Api.sendMedia(
    group: String,
    photos: List<Uri>,
    message: Message? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    val scaledPhotos = photos.map {
        it.asScaledJpeg(context)
    }
    return post("groups/$group/photos", MultiPartFormDataContent(
        formData {
            if (message != null) {
                append("message", json.encodeToString(message))
            }
            scaledPhotos.forEachIndexed { index, photo ->
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
    ), client = dataClient(), onError = onError, onSuccess = onSuccess)
}

suspend fun Api.sendAudio(
    group: String,
    audio: File,
    message: Message? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) {
    return post("groups/$group/audio", MultiPartFormDataContent(
        formData {
            if (message != null) {
                append("message", json.encodeToString(message))
            }
            append(
                "audio",
                audio.asInputProvider(),
                Headers.build {
                    append(HttpHeaders.ContentType, "audio/mp4")
                    append(HttpHeaders.ContentDisposition, "filename=audio.m4a")
                }
            )
        }
    ), client = dataClient(), onError = onError, onSuccess = onSuccess)
}

suspend fun Api.sendVideos(
    group: String,
    videos: List<Uri>,
    message: Message? = null,
    processingCallback: (Float) -> Unit,
    uploadCallback: (Float) -> Unit,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    val scaledVideos = try {
        videos.map { it.asScaledVideo(context, progressCallback = processingCallback) to context.contentResolver.getType(it) }
    } catch (e: Exception) {
        e.printStackTrace()
        onError?.invoke(e)
        return
    }
    return post(
        "groups/$group/videos",
        MultiPartFormDataContent(
            formData {
                if (message != null) {
                    append("message", json.encodeToString(message))
                }
                scaledVideos.forEachIndexed { index, video ->
                    append(
                        "photo[$index]",
                        video.first.asInputProvider(),
                        Headers.build {
                            append(HttpHeaders.ContentType, video.second ?: "video/*")
                            append(HttpHeaders.ContentDisposition, "filename=${video.second?.split("/")?.lastOrNull() ?: ""}")
                        }
                    )
                }
            }
        ),
        progressCallback = uploadCallback,
        client = dataClient(),
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
