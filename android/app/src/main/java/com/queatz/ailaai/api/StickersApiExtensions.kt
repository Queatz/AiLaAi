package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import app.ailaai.api.createSticker
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.db.Sticker


suspend fun Api.createStickerFromUri(
    id: String,
    photo: Uri,
    context: Context,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Sticker>
) {
    createSticker(
        id,
        try {
            photo.asInputProvider(context, maxSize = 500_000)!!
        } catch (e: Throwable) {
            onError?.invoke(e)
            return
        },
        context.contentResolver.getType(photo) ?: "image/png",
        onError,
        onSuccess
    )
}
