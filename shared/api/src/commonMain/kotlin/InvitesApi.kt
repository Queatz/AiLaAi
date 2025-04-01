package app.ailaai.api

import com.queatz.db.Invite

suspend fun Api.createQuickInvite(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite> = {}
) = get(
    url = "invite",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createInvite(
    invite: Invite,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite> = {}
) = post(
    url = "invite",
    body = invite,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.invite(
    code: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite> = {}
) = get(
    url = "invite/$code",
    onError = onError,
    onSuccess = onSuccess
)
