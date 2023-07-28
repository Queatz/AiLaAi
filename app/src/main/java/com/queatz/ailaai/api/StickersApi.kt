package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.asInputProvider
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.stickerPack(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StickerPack>
) = get("sticker-packs/${id}", onError = onError, onSuccess = onSuccess)

suspend fun Api.sticker(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Sticker>
) = get("sticker/${id}", onError = onError, onSuccess = onSuccess)

suspend fun Api.myStickerPacks(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<StickerPack>>
) = get("me/sticker-packs", onError = onError, onSuccess = onSuccess)

suspend fun Api.stickerPacks(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<StickerPack>>
) = get("sticker-packs", onError = onError, onSuccess = onSuccess)

suspend fun Api.saveStickerPack(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("sticker-packs/${id}/save", onError = onError, onSuccess = onSuccess)

suspend fun Api.unsaveStickerPack(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("sticker-packs/${id}/unsave", onError = onError, onSuccess = onSuccess)

suspend fun Api.createStickerPack(
    stickerPack: StickerPack,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StickerPack>
) = post("sticker-packs", stickerPack, onError = onError, onSuccess = onSuccess)

suspend fun Api.updateStickerPack(
    id: String,
    stickerPack: StickerPack,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StickerPack>
) = post("sticker-packs/${id}", stickerPack, onError = onError, onSuccess = onSuccess)

suspend fun Api.stickers(
    stickerPackId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Sticker>>
) = get("sticker-packs/${stickerPackId}/stickers", onError = onError, onSuccess = onSuccess)

suspend fun Api.createSticker(
    id: String,
    photo: Uri,
    context: Context,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Sticker>
) = post("sticker-packs/${id}/stickers", MultiPartFormDataContent(
    formData {
        append("photo", photo.asInputProvider(context, maxSize = 500_000)!!, Headers.build {
            append(HttpHeaders.ContentType, context.contentResolver.getType(photo) ?: "image/png")
            append(HttpHeaders.ContentDisposition, "filename=sticker.${context.contentResolver.getType(photo)?.split("/")?.lastOrNull() ?: "png"}")
        })
    }
), client = dataClient(), onError = onError, onSuccess = onSuccess)

suspend fun Api.updateSticker(
    id: String,
    sticker: Sticker,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Sticker>
) = post("sticker/${id}", sticker, onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteSticker(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("sticker/${id}/delete", onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteStickerPack(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("sticker-pack/${id}/delete", onError = onError, onSuccess = onSuccess)
