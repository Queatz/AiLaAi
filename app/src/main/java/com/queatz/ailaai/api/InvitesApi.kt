package com.queatz.ailaai.api

import com.queatz.ailaai.Api
import com.queatz.ailaai.ErrorBlock
import com.queatz.ailaai.Invite
import com.queatz.ailaai.SuccessBlock

suspend fun Api.invite(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite> = {}
) = get(
    "invite",
    onError = onError,
    onSuccess = onSuccess
)
