package app.ailaai.api

import com.queatz.db.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlin.time.Instant

suspend fun Api.cards(
    geo: Geo,
    altitude: Double? = null,
    offset: Int = 0,
    limit: Int = 20,
    search: String? = null,
    public: Boolean = false,
    availableToday: Boolean? = null,
    availableTomorrow: Boolean? = null,
    pets: Boolean? = null,
    outdoors: Boolean? = null,
    languages: List<String>? = null,
    minAge: Int? = null,
    maxAge: Int? = null,
    minGroupSize: Int? = null,
    maxGroupSize: Int? = null,
    activityActive: Boolean? = null,
    parking: Parking? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) =
    get("cards", mapOf(
        "geo" to geo.toString(),
        "altitude" to altitude?.toString(),
        "offset" to offset.toString(),
        "limit" to limit.toString(),
        "public" to public.toString(),
        "search" to search,
        "availableToday" to availableToday?.toString(),
        "availableTomorrow" to availableTomorrow?.toString(),
        "pets" to pets?.toString(),
        "outdoors" to outdoors?.toString(),
        "languages" to languages?.joinToString(","),
        "minAge" to minAge?.toString(),
        "maxAge" to maxAge?.toString(),
        "minGroupSize" to minGroupSize?.toString(),
        "maxGroupSize" to maxGroupSize?.toString(),
        "activityActive" to activityActive?.toString(),
        "parking" to parking?.name,
    ),
        errorContext = "load pages",
        onError = onError,
        onSuccess = onSuccess
    )

suspend fun Api.card(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Card>,
) = get(
    "cards/$id",
    errorContext = "load page",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.cardByUrl(
    url: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Card>,
) = get(
    "urls/cards/$url",
    errorContext = "load page",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.cardsCards(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get(
    "cards/$id/cards",
    errorContext = "load pages",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.myCards(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get(
    "me/cards",
    errorContext = "load my pages",
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
    errorContext = "load saved pages",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.newCard(
    card: Card? = Card(),
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Card>,
) = post(
    "cards", card,
    errorContext = "create page",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateCard(
    id: String,
    card: Card,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Card> = {},
) = post(
    url = "cards/$id",
    body = card,
    errorContext = "update page",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.upgradeCardDetails(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<CardUpgradeDetails>,
) = get(
    url = "cards/$id/upgrade",
    errorContext = "load upgrade details",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.upgradeCard(
    id: String,
    upgrade: CardUpgradeBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    url = "cards/$id/upgrade",
    body = upgrade,
    errorContext = "upgrade page",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.downgradeCardDetails(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<CardDowngradeDetails>,
) = get(
    url = "cards/$id/downgrade",
    errorContext = "load downgrade details",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.downgradeCard(
    id: String,
    downgrade: CardDowngradeBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    url = "cards/$id/downgrade",
    body = downgrade,
    errorContext = "downgrade page",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteCard(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    "cards/$id/delete",
    errorContext = "delete page",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.saveCard(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    "cards/$id/save",
    errorContext = "save page",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.unsaveCard(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    "cards/$id/unsave",
    errorContext = "unsave page",
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
        errorContext = "upload page image",
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
        errorContext = "upload page video",
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
    errorContext = "load page group",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.cardPeople(
    card: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Person>> = {},
) = get(
    "cards/$card/people",
    errorContext = "load page people",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.cardVisits(
    card: String,
    since: Instant,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<CardVisit>> = {},
) = get(
    url = "cards/$card/visits",
    parameters = mapOf(
        "since" to since.toString()
    ),
    errorContext = "load page visits",
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
    errorContext = "send reply",
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
        errorContext = "upload image",
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
        errorContext = "upload audio",
        onError = onError,
        onSuccess = onSuccess
    )
}
