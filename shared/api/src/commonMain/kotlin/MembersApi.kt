package app.ailaai.api

import com.queatz.db.Member
import io.ktor.http.*

suspend fun Api.createMember(
    member: Member,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Member> = {}
) = post("members", member, errorContext = "add member", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateMember(
    id: String,
    member: Member,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("members/$id", member, errorContext = "update member", onError = onError, onSuccess = onSuccess)

suspend fun Api.removeMember(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("members/$id/delete", errorContext = "remove member", onError = onError, onSuccess = onSuccess)
