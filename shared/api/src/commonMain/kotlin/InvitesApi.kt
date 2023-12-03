package app.ailaai.api

import com.queatz.db.Invite

suspend fun Api.invite(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite> = {}
) = get(
    "invite",
    onError = onError,
    onSuccess = onSuccess
)
