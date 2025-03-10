package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg


suspend fun Api.uploadStoryPhotosFromUri(
    context: Context,
    story: String,
    media: List<Uri>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>
) {
    val scaledPhotos = media.mapNotNull {
        it.asScaledJpeg(context)
    }
    return uploadStoryPhotos(
        story = story,
        media = scaledPhotos,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadStoryAudioFromUri(
    context: Context,
    story: String,
    audio: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<String>
) {
    return uploadStoryAudio(
        story = story,
        audio = audio.asInputProvider(context) ?: throw Exception("Couldn't load audio file"),
        onError = onError,
        onSuccess = onSuccess
    )
}
