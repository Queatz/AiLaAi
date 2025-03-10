package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import app.ailaai.api.*
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg
import com.queatz.ailaai.extensions.asScaledVideo
import io.ktor.http.*
import uploadProfileContentAudio
import uploadProfileContentPhotos

suspend fun Api.uploadCardPhotoFromUri(
    context: Context,
    id: String,
    photo: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return uploadCardPhoto(
        id = id,
        photo = photo.asScaledJpeg(context) ?: run {
            onError?.invoke(IllegalStateException("Uri returned null photo"))
            return
        },
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadCardVideoFromUri(
    context: Context,
    id: String,
    video: Uri,
    contentType: String,
    filename: String,
    processingCallback: (Float) -> Unit,
    uploadCallback: (Float) -> Unit,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    val scaledVideo = try {
        video.asScaledVideo(context, progressCallback = processingCallback).asInputProvider()
    } catch (e: Exception) {
        e.printStackTrace()
        onError?.invoke(e)
        return
    }
    return uploadCardVideo(
        id = id,
        video = scaledVideo,
        contentType = contentType,
        filename = filename,
        uploadCallback = uploadCallback,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadCardContentPhotosFromUri(
    context: Context,
    card: String,
    media: List<Uri>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>
) {
    val scaledPhotos = media.mapNotNull {
        it.asScaledJpeg(context)
    }
    return uploadCardContentPhotos(
        card = card,
        media = scaledPhotos,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadCardContentAudioFromUri(
    context: Context,
    card: String,
    audio: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<String>
) {
    return uploadCardContentAudio(
        card = card,
        audio = audio.asInputProvider(context) ?: throw Exception("Couldn't load audio file"),
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadProfileContentPhotosFromUri(
    context: Context,
    card: String,
    media: List<Uri>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>
) {
    val scaledPhotos = media.mapNotNull {
        it.asScaledJpeg(context)
    }
    return uploadProfileContentPhotos(
        profile = card,
        media = scaledPhotos,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadProfileContentAudioFromUri(
    context: Context,
    card: String,
    audio: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<String>
) {
    return uploadProfileContentAudio(
        profile = card,
        audio = audio.asInputProvider(context) ?: throw Exception("Couldn't load audio file"),
        onError = onError,
        onSuccess = onSuccess
    )
}
