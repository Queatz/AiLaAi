package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import app.ailaai.api.*
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg
import com.queatz.ailaai.extensions.asScaledVideo
import io.ktor.http.*

suspend fun Api.uploadCardPhotoFromUri(
    context: Context,
    id: String,
    photo: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return uploadCardPhoto(
        id,
        photo.asScaledJpeg(context),
        onError,
        onSuccess
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
        id,
        scaledVideo,
        contentType,
        filename,
        uploadCallback,
        onError,
        onSuccess
    )
}
