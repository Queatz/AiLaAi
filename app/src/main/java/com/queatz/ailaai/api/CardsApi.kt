package com.queatz.ailaai.api

import android.net.Uri
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg
import com.queatz.ailaai.extensions.asScaledVideo
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.cards(
    geo: LatLng,
    offset: Int = 0,
    limit: Int = 20,
    search: String? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) =
    get("cards", mapOf(
        "geo" to "${geo.latitude},${geo.longitude}",
        "offset" to offset.toString(),
        "limit" to limit.toString()
    ) + (search?.let {
        mapOf("search" to search)
    } ?: mapOf()),
        onError = onError,
        onSuccess = onSuccess)

suspend fun Api.card(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Card>,
) = get(
    "cards/$id",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.cardsCards(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get(
    "cards/$id/cards",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.myCards(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get(
    "me/cards",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.savedCards(
    search: String? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<SaveAndCard>>,
) = get("me/saved", search?.let {
    mapOf("search" to search)
} ?: mapOf(),
    onError = onError,
    onSuccess = onSuccess)

suspend fun Api.newCard(
    card: Card? = Card(offline = true),
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Card>,
) = post(
    "cards", card,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateCard(
    id: String,
    card: Card,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Card> = {},
) = post(
    "cards/$id", card,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteCard(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    "cards/$id/delete",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.saveCard(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    "cards/$id/save",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.unsaveCard(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    "cards/$id/unsave",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.uploadCardPhoto(
    id: String,
    photo: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    val scaledPhoto = photo.asScaledJpeg(context)
    return post("cards/$id/photo", MultiPartFormDataContent(
        formData {
            append("photo", scaledPhoto, Headers.build {
                append(HttpHeaders.ContentType, "image/jpg")
                append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
            })
        }
    ), client = dataClient(),
        onError = onError,
        onSuccess = onSuccess)
}

suspend fun Api.uploadCardVideo(
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
        video.asScaledVideo(context, progressCallback = processingCallback)
    } catch (e: Exception) {
        e.printStackTrace()
        onError?.invoke(e)
        return
    }
    return post(
        "cards/$id/video",
        MultiPartFormDataContent(
            formData {
                append(
                    "photo",
                    scaledVideo.asInputProvider(),
                    Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=${filename}")
                    }
                )
            }
        ),
        progressCallback = uploadCallback,
        client = dataClient(),
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.cardGroup(
    card: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Group>,
) = get(
    "cards/$card/group",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.cardPeople(
    card: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Person>> = {},
) = get(
    "cards/$card/people",
    onError = onError,
    onSuccess = onSuccess
)
