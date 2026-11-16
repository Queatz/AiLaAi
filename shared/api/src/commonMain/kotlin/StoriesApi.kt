package com.queatz.ailaai.api

import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.Comment
import com.queatz.db.CommentExtended
import com.queatz.db.Geo
import com.queatz.db.ReactBody
import com.queatz.db.ReactionAndPerson
import com.queatz.db.Story
import com.queatz.db.StoryDraft
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.stories(
    geo: Geo,
    offset: Int = 0,
    limit: Int = 20,
    public: Boolean? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Story>>
) = get(
    "stories",
    mapOf(
        "geo" to geo.toString(),
        "offset" to offset.toString(),
        "limit" to limit.toString(),
        "public" to public?.toString()
    ),
    errorContext = "load posts",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createStory(
    story: Story,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Story>
) = post("stories", story, errorContext = "create post", onError = onError, onSuccess = onSuccess)

suspend fun Api.story(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Story>
) = get("stories/$id", errorContext = "load post", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateStory(
    id: String,
    story: Story,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Story> = {}
) = post("stories/$id", story, errorContext = "update post", onError = onError, onSuccess = onSuccess)

suspend fun Api.myStories(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Story>>
) = get("me/stories", errorContext = "load my posts", onError = onError, onSuccess = onSuccess)

suspend fun Api.uploadStoryPhotos(
    story: String,
    media: List<ByteArray>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>
) {
    return post(
        "stories/$story/photos",
        MultiPartFormDataContent(
            formData {
                media.forEachIndexed { index, photo ->
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
        ),
        client = httpDataClient,
        errorContext = "upload image",
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadStoryAudio(
    story: String,
    audio: InputProvider,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<String>
) {
    return post(
        "stories/$story/audio",
        MultiPartFormDataContent(
            formData {
                append(
                    "audio",
                    audio,
                    Headers.build {
                        append(HttpHeaders.ContentType, "audio/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=audio.m4a")
                    }
                )
            }
        ),
        client = httpDataClient,
        errorContext = "upload audio",
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.deleteStory(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("stories/$id/delete", errorContext = "delete post", onError = onError, onSuccess = onSuccess)

suspend fun Api.reactToStory(
    id: String,
    react: ReactBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("stories/$id/react", react, errorContext = "react to post", onError = onError, onSuccess = onSuccess)

suspend fun Api.commentOnStory(
    id: String,
    comment: Comment,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Comment> = {}
) = post("stories/$id/comment", comment, errorContext = "add comment", onError = onError, onSuccess = onSuccess)

suspend fun Api.storyReactions(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<ReactionAndPerson>> = {}
) = get("stories/$id/reactions", errorContext = "load reactions", onError = onError, onSuccess = onSuccess)

suspend fun Api.storyComments(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<CommentExtended>> = {}
) = get("stories/$id/comments", errorContext = "load comments", onError = onError, onSuccess = onSuccess)

suspend fun Api.storyDraft(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StoryDraft>
) = get("stories/$id/draft", errorContext = "load draft", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateStoryDraft(
    id: String,
    draft: StoryDraft,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StoryDraft>
) = post("stories/$id/draft", draft, errorContext = "save draft", onError = onError, onSuccess = onSuccess)

suspend fun Api.storyByUrl(
    url: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<Story>
) = get(
    url = "urls/stories/$url",
    errorContext = "load post",
    onError = onError,
    onSuccess = onSuccess
)
