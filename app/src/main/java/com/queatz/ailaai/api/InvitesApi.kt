package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.Invite
import com.queatz.ailaai.data.SuccessBlock

suspend fun Api.invite(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite> = {}
) = get(
    "invite",
    onError = onError,
    onSuccess = onSuccess
)
