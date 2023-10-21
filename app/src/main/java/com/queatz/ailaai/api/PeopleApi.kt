package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.SuccessBlock
import com.queatz.db.Card
import com.queatz.db.Person
import com.queatz.db.PersonProfile

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

suspend fun Api.profileCards(
    personId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>>,
) = get("people/$personId/profile/cards", onError = onError, onSuccess = onSuccess)
