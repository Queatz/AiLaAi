package app.ailaai.api

import com.queatz.db.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.cards(
    geo: Geo,
    offset: Int = 0,
    limit: Int = 20,
    search: String? = null,
    paid: Boolean? = null,
    public: Boolean = false,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) =
    get("cards", mapOf(
        "geo" to geo.toString(),
        "offset" to offset.toString(),
        "limit" to limit.toString(),
        "public" to public.toString(),
        "search" to search,
        "paid" to paid?.toString()
    ),
        onError = onError,
        onSuccess = onSuccess
    )

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
    offset: Int = 0,
    limit: Int = 20,
    search: String? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<SaveAndCard>>,
) = get(
    "me/saved",
    mapOf(
        "search" to search,
        "offset" to offset.toString(),
        "limit" to limit.toString()
    ),
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.newCard(
    card: Card? = Card(),
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

suspend fun Api.generateCardPhoto(
    cardId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    url = "cards/$cardId/photo/generate",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.uploadCardPhoto(
    id: String,
    photo: ByteArray,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return post("cards/$id/photo", MultiPartFormDataContent(
        formData {
            append("photo", photo, Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
            })
        }
    ), client = httpDataClient,
        onError = onError,
        onSuccess = onSuccess)
}

suspend fun Api.uploadCardVideo(
    id: String,
    video: InputProvider,
    contentType: String,
    filename: String,
    uploadCallback: (Float) -> Unit,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return post(
        "cards/$id/video",
        MultiPartFormDataContent(
            formData {
                append(
                    "photo",
                    video,
                    Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=${filename}")
                    }
                )
            }
        ),
        progressCallback = uploadCallback,
        client = httpDataClient,
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

suspend fun Api.wildReply(
    reply: WildReplyBody,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post(
    url = "wild/reply",
    body = reply,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.uploadCardContentPhotos(
    card: String,
    media: List<ByteArray>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>
) {
    return post(
        "cards/$card/content/photos",
        MultiPartFormDataContent(
            formData {
                media.forEachIndexed { index, photo ->
                    append(
                        "photo[$index]",
                        photo,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
                        }
                    )
                }
            }
        ),
        client = httpDataClient,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadCardContentAudio(
    card: String,
    audio: InputProvider,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<String>
) {
    return post(
        "cards/$card/content/audio",
        MultiPartFormDataContent(
            formData {
                append(
                    "audio",
                    audio,
                    Headers.build {
                        append(HttpHeaders.ContentType, "audio/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=audio.m4a")
                    }
                )
            }
        ),
        client = httpDataClient,
        onError = onError,
        onSuccess = onSuccess
    )
}
