package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg


suspend fun Api.uploadStoryPhotos(
    context: Context,
    story: String,
    media: List<Uri>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>
) {
    val scaledPhotos = media.map {
        it.asScaledJpeg(context)
    }
    return uploadStoryPhotos(
        story,
        scaledPhotos,
        onError,
        onSuccess
    )
}

suspend fun Api.uploadStoryAudio(
    context: Context,
    story: String,
    audio: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<String>
) {
    return uploadStoryAudio(
        story,
        audio.asInputProvider(context) ?: throw Exception("Couldn't load audio file"),
        onError,
        onSuccess
    )
}
