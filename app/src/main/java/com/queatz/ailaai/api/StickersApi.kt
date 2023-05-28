package com.queatz.ailaai.api

import android.content.Context
import android.net.Uri
import com.queatz.ailaai.Api
import com.queatz.ailaai.Sticker
import com.queatz.ailaai.StickerPack
import com.queatz.ailaai.extensions.asInputProvider
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.stickerPack(id: String): StickerPack = get("sticker-packs/${id}")
suspend fun Api.sticker(id: String): Sticker = get("sticker/${id}")
suspend fun Api.myStickerPacks(): List<StickerPack> = get("me/sticker-packs")
suspend fun Api.stickerPacks(): List<StickerPack> = get("sticker-packs")
suspend fun Api.saveStickerPack(id: String): HttpStatusCode = post("sticker-packs/${id}/save")
suspend fun Api.unsaveStickerPack(id: String): HttpStatusCode = post("sticker-packs/${id}/unsave")
suspend fun Api.createStickerPack(stickerPack: StickerPack): StickerPack = post("sticker-packs", stickerPack)
suspend fun Api.updateStickerPack(id: String, stickerPack: StickerPack): StickerPack = post("sticker-packs/${id}", stickerPack)
suspend fun Api.stickers(stickerPackId: String): List<Sticker> = get("sticker-packs/{id}/stickers")
suspend fun Api.createSticker(id: String, photo: Uri, context: Context): Sticker = post("sticker-packs/${id}/stickers", MultiPartFormDataContent(
    formData {
        append("photo", photo.asInputProvider(context, maxSize = 500_000)!!, Headers.build {
            append(HttpHeaders.ContentType, context.contentResolver.getType(photo) ?: "image/png")
            append(HttpHeaders.ContentDisposition, "filename=sticker.png")
        })
    }
), client = dataClient())
suspend fun Api.updateSticker(id: String, sticker: Sticker): Sticker = post("sticker/${id}", sticker)
suspend fun Api.deleteSticker(id: String): HttpStatusCode = post("sticker/${id}/delete")
suspend fun Api.deleteStickerPack(id: String): HttpStatusCode = post("sticker-pack/${id}/delete")
