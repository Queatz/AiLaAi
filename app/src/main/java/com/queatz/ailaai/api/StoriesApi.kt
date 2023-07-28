package com.queatz.ailaai.api

import android.net.Uri
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.stories(
    geo: LatLng,
    offset: Int = 0,
    limit: Int = 20,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Story>>
) = get(
    "stories",
    mapOf(
        "geo" to "${geo.latitude},${geo.longitude}",
        "offset" to offset.toString(),
        "limit" to limit.toString()
    ),
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createStory(
    story: Story,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Story>
) = post("stories", story, onError = onError, onSuccess = onSuccess)

suspend fun Api.story(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Story>
) = get("stories/$id", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateStory(
    id: String,
    story: Story,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Story>
) = post("stories/$id", story, onError = onError, onSuccess = onSuccess)

suspend fun Api.myStories(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Story>>
) = get("me/stories", onError = onError, onSuccess = onSuccess)

suspend fun Api.uploadStoryPhotos(
    story: String,
    media: List<Uri>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>
) {
    val scaledPhotos = media.map {
        it.asScaledJpeg(context)
    }
    return post(
        "stories/$story/photos",
        MultiPartFormDataContent(
            formData {
                scaledPhotos.forEachIndexed { index, photo ->
                    append(
                        "photo[$index]",
                        photo,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpg")
                            append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
                        }
                    )
                }
            }
        ),
        client = dataClient(),
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadStoryAudio(
    story: String,
    audio: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<String>
) {
    return post(
        "stories/$story/audio",
        MultiPartFormDataContent(
            formData {
                append(
                    "audio",
                    audio.asInputProvider(context) ?: throw Exception("Couldn't load audio file"),
                    Headers.build {
                        append(HttpHeaders.ContentType, "audio/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=audio.m4a")
                    }
                )
            }
        ),
        client = dataClient(),
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.deleteStory(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("stories/$id/delete", onError = onError, onSuccess = onSuccess)

suspend fun Api.storyDraft(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StoryDraft>
) = get("stories/$id/draft", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateStoryDraft(
    id: String,
    draft: StoryDraft,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StoryDraft>
) = post("stories/$id/draft", draft, onError = onError, onSuccess = onSuccess)
