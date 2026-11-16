package app.ailaai.api

import com.queatz.db.*
import io.ktor.http.*

suspend fun Api.people(
    search: String? = null,
    geo: List<Double>? = null,
    offset: Int = 0,
    limit: Int = 20,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<PersonProfile>>,
) {
    val parameters = mutableMapOf<String, String>()

    if (search != null) {
        parameters["search"] = search
    }

    if (geo != null && geo.size >= 2) {
        parameters["geo"] = "${geo[0]},${geo[1]}"
    }

    parameters["offset"] = offset.toString()
    parameters["limit"] = limit.toString()

    get("people", parameters, errorContext = "load people", onError = onError, onSuccess = onSuccess)
}

suspend fun Api.profile(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PersonProfile>,
) = get("people/$personId/profile", errorContext = "load profile", onError = onError, onSuccess = onSuccess)

suspend fun Api.groupsOfPerson(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GroupExtended>>,
) = get("people/$personId/groups", errorContext = "load groups", onError = onError, onSuccess = onSuccess)

suspend fun Api.statusesOfPerson(
    personId: String,
    offset: Int = 0,
    limit: Int = 20,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<PersonStatus>>,
) = get(
    url = "people/$personId/statuses",
    parameters = mapOf("offset" to offset.toString(), "limit" to limit.toString()),
    errorContext = "load statuses",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.storiesOfPerson(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Story>>,
) = get("people/$personId/stories", errorContext = "load posts", onError = onError, onSuccess = onSuccess)

suspend fun Api.profileCards(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get("people/$personId/profile/cards", errorContext = "load pages", onError = onError, onSuccess = onSuccess)

suspend fun Api.activeCardsOfPerson(
    personId: String,
    search: String?,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get("people/$personId/profile/cards", mapOf("search" to search), errorContext = "load pages", onError = onError, onSuccess = onSuccess)

suspend fun Api.equippedItems(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<InventoryItemExtended>>,
) = get("people/$personId/items/equipped", errorContext = "load equipped items", onError = onError, onSuccess = onSuccess)

suspend fun Api.profileByUrl(
    url: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<PersonProfile>
) = get(
    url = "profile/url/$url",
    errorContext = "load profile",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.subscribe(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Subscription>,
) = post("people/$personId/subscribe", errorContext = "subscribe", onError = onError, onSuccess = onSuccess)

suspend fun Api.unsubscribe(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post("people/$personId/unsubscribe", errorContext = "unsubscribe", onError = onError, onSuccess = onSuccess)
