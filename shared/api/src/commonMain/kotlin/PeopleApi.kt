package app.ailaai.api

import com.queatz.db.*
import io.ktor.http.*

suspend fun Api.people(
    search: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Person>>,
) = get("people", mapOf("search" to search), onError = onError, onSuccess = onSuccess)

suspend fun Api.profile(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PersonProfile>,
) = get("people/$personId/profile", onError = onError, onSuccess = onSuccess)

suspend fun Api.groupsOfPerson(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GroupExtended>>,
) = get("people/$personId/groups", onError = onError, onSuccess = onSuccess)

suspend fun Api.statusesOfPerson(
    personId: String,
    offset: Int = 0,
    limit: Int = 20,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<PersonStatus>>,
) = get(
    url = "people/$personId/statuses",
    parameters = mapOf("offset" to offset.toString(), "limit" to limit.toString()),
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.storiesOfPerson(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Story>>,
) = get("people/$personId/stories", onError = onError, onSuccess = onSuccess)

suspend fun Api.profileCards(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get("people/$personId/profile/cards", onError = onError, onSuccess = onSuccess)

suspend fun Api.activeCardsOfPerson(
    personId: String,
    search: String?,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get("people/$personId/profile/cards", mapOf("search" to search), onError = onError, onSuccess = onSuccess)

suspend fun Api.equippedItems(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<InventoryItemExtended>>,
) = get("people/$personId/items/equipped", onError = onError, onSuccess = onSuccess)

suspend fun Api.profileByUrl(
    url: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<PersonProfile>
) = get(
    url = "profile/url/$url",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.subscribe(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Subscription>,
) = post("people/$personId/subscribe", onError = onError, onSuccess = onSuccess)

suspend fun Api.unsubscribe(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post("people/$personId/unsubscribe", onError = onError, onSuccess = onSuccess)
