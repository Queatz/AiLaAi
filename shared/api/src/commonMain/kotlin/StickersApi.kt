package app.ailaai.api

import com.queatz.db.Sticker
import com.queatz.db.StickerPack
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.stickerPack(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StickerPack>
) = get("sticker-packs/${id}", errorContext = "load sticker pack", onError = onError, onSuccess = onSuccess)

suspend fun Api.sticker(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Sticker>
) = get("sticker/${id}", errorContext = "load sticker", onError = onError, onSuccess = onSuccess)

suspend fun Api.myStickerPacks(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<StickerPack>>
) = get("me/sticker-packs", errorContext = "load sticker packs", onError = onError, onSuccess = onSuccess)

suspend fun Api.stickerPacks(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<StickerPack>>
) = get("sticker-packs", errorContext = "load sticker packs", onError = onError, onSuccess = onSuccess)

suspend fun Api.saveStickerPack(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("sticker-packs/${id}/save", errorContext = "save sticker pack", onError = onError, onSuccess = onSuccess)

suspend fun Api.unsaveStickerPack(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("sticker-packs/${id}/unsave", errorContext = "unsave sticker pack", onError = onError, onSuccess = onSuccess)

suspend fun Api.createStickerPack(
    stickerPack: StickerPack,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StickerPack>
) = post("sticker-packs", stickerPack, errorContext = "create sticker pack", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateStickerPack(
    id: String,
    stickerPack: StickerPack,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StickerPack>
) = post("sticker-packs/${id}", stickerPack, errorContext = "update sticker pack", onError = onError, onSuccess = onSuccess)

suspend fun Api.stickers(
    stickerPackId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Sticker>>
) = get("sticker-packs/${stickerPackId}/stickers", errorContext = "load stickers", onError = onError, onSuccess = onSuccess)

suspend fun Api.createSticker(
    id: String,
    photo: InputProvider,
    photoContentType: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Sticker>
) = post("sticker-packs/${id}/stickers", MultiPartFormDataContent(
    formData {
        append("photo", photo, Headers.build {
            append(HttpHeaders.ContentType, photoContentType)
            append(HttpHeaders.ContentDisposition, "filename=sticker.${photoContentType.split("/").lastOrNull() ?: "png"}")
        })
    }
), client = httpDataClient, errorContext = "create sticker", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateSticker(
    id: String,
    sticker: Sticker,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Sticker>
) = post("sticker/${id}", sticker, errorContext = "update sticker", onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteSticker(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("sticker/${id}/delete", errorContext = "delete sticker", onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteStickerPack(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("sticker-pack/${id}/delete", errorContext = "delete sticker pack", onError = onError, onSuccess = onSuccess)
