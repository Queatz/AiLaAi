package com.queatz.ailaai.api

import com.queatz.ailaai.Api
import com.queatz.ailaai.Card
import com.queatz.ailaai.ErrorBlock
import com.queatz.ailaai.SuccessBlock
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
private data class LeaveCollaborationBody(val card: String)

suspend fun Api.myCollaborations(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Card>> = {}
) = get(
    "me/collaborations",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.leaveCollaboration(
    card: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post(
    "me/collaborations/leave",
    LeaveCollaborationBody(card),
    onError = onError,
    onSuccess = onSuccess
)
