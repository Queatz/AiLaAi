package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import app.ailaai.api.uploadPhotos
import app.ailaai.api.uploadVideo
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg
import com.queatz.ailaai.extensions.asScaledVideo
import com.queatz.db.UploadResponse

suspend fun Api.uploadPhotosFromUris(
    context: Context,
    photos: List<Uri>,
    removeBackground: Boolean = false,
    crop: Boolean = false,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<UploadResponse> = {},
) {
    val scaledPhotos = photos.mapNotNull {
        it.asScaledJpeg(context)
    }
    return uploadPhotos(
        photos = scaledPhotos,
        removeBackground = removeBackground,
        crop = crop,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadVideoFromUri(
    context: Context,
    video: Uri,
    processingCallback: (Float) -> Unit = {},
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<UploadResponse>,
) {
    val scaledVideo = try {
        video.asScaledVideo(context, progressCallback = processingCallback).asInputProvider()
    } catch (e: Exception) {
        e.printStackTrace()
        onError?.invoke(e)
        return
    }
    uploadVideo(
        video = scaledVideo,
        contentType = context.contentResolver.getType(video) ?: "video/*",
        filename = video.lastPathSegment ?: "video.${
            context.contentResolver.getType(video)?.split("/")?.lastOrNull() ?: ""
        }",
        onError = onError,
        onSuccess = onSuccess
    )
}
