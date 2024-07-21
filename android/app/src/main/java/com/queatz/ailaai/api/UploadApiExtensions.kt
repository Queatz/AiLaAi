package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import app.ailaai.api.uploadPhotos
import com.queatz.ailaai.extensions.asScaledJpeg
import com.queatz.db.UploadResponse

suspend fun Api.uploadPhotosFromUris(
    context: Context,
    photos: List<Uri>,
    removeBackground: Boolean = false,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<UploadResponse> = {},
) {
    val scaledPhotos = photos.mapNotNull {
        it.asScaledJpeg(context)
    }
    return uploadPhotos(
        scaledPhotos,
        removeBackground,
        onError,
        onSuccess
    )
}
