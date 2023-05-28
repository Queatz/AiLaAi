package com.queatz.ailaai.api

import android.net.Uri
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.Api
import com.queatz.ailaai.Story
import com.queatz.ailaai.StoryDraft
import com.queatz.ailaai.asScaledJpeg
import com.queatz.ailaai.extensions.asInputProvider
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.stories(geo: LatLng, offset: Int = 0, limit: Int = 20): List<Story> = get(
    "stories",
    mapOf(
        "geo" to "${geo.latitude},${geo.longitude}",
        "offset" to offset.toString(),
        "limit" to limit.toString()
    )
)

suspend fun Api.createStory(story: Story): Story = post("stories", story)

suspend fun Api.story(id: String): Story = get("stories/$id")

suspend fun Api.updateStory(id: String, story: Story): Story = post("stories/$id", story)

suspend fun Api.myStories(): List<Story> = get("me/stories")

suspend fun Api.uploadStoryPhotos(story: String, media: List<Uri>): List<String> {
    val scaledPhotos = media.map {
        it.asScaledJpeg(context)
    }
    return post("stories/$story/photos", MultiPartFormDataContent(
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
    ), client = dataClient())
}

suspend fun Api.uploadStoryAudio(story: String, audio: Uri): String {
    return post("stories/$story/audio", MultiPartFormDataContent(
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
    ), client = dataClient())
}

suspend fun Api.deleteStory(id: String): HttpStatusCode = post("stories/$id/delete")

suspend fun Api.storyDraft(id: String): StoryDraft = get("stories/$id/draft")

suspend fun Api.updateStoryDraft(id: String, draft: StoryDraft): StoryDraft = post("stories/$id/draft", draft)
