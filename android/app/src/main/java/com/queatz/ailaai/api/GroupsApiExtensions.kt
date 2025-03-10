package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.ScaledVideo
import app.ailaai.api.SuccessBlock
import app.ailaai.api.sendAudio
import app.ailaai.api.sendMedia
import app.ailaai.api.sendVideos
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg
import com.queatz.ailaai.extensions.asScaledVideo
import com.queatz.db.Message
import io.ktor.http.HttpStatusCode
import java.io.File


suspend fun Api.sendMediaFromUri(
    context: Context,
    group: String,
    photos: List<Uri>,
    message: Message? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    val scaledPhotos = photos.mapNotNull {
        it.asScaledJpeg(context)
    }
    return sendMedia(
        group = group,
        photos = scaledPhotos,
        message = message,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.sendAudioFromUri(
    group: String,
    audio: File,
    message: Message? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) {
    return sendAudio(
        group = group,
        audio = audio.asInputProvider(),
        message = message,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.sendVideosFromUri(
    context: Context,
    group: String,
    videos: List<Uri>,
    message: Message? = null,
    processingCallback: (Float) -> Unit,
    uploadCallback: (Float) -> Unit,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    val scaledVideos = try {
        videos.map {
            ScaledVideo(
                inputProvider = it.asScaledVideo(
                    context = context,
                    progressCallback = processingCallback
                ).asInputProvider(),
                type = context.contentResolver.getType(it) ?: "video/*",
                fileName = it.lastPathSegment
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onError?.invoke(e)
        return
    }
    return sendVideos(
        group = group,
        videos = scaledVideos,
        message = message,
        uploadCallback = uploadCallback,
        onError = onError,
        onSuccess = onSuccess
    )
}

