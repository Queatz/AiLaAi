package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import app.ailaai.api.*
import com.queatz.ailaai.extensions.ScaledVideo
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg
import com.queatz.ailaai.extensions.asScaledVideo
import com.queatz.db.Message
import io.ktor.http.*
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
        group,
        scaledPhotos,
        message,
        onError,
        onSuccess
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
        group,
        audio.asInputProvider(),
        message,
        onError,
        onSuccess
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
                it.asScaledVideo(
                    context,
                    progressCallback = processingCallback
                ).asInputProvider(),
                context.contentResolver.getType(it) ?: "video/*",
                it.toFile().name
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onError?.invoke(e)
        return
    }
    return sendVideos(
        group,
        scaledVideos,
        message,
        uploadCallback,
        onError,
        onSuccess
    )
}

