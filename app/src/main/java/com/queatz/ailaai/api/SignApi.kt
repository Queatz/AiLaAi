package com.queatz.ailaai.api

import com.queatz.ailaai.data.*

suspend fun Api.signUp(
    inviteCode: String?,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<TokenResponse> = {},
) = post("sign/up", SignUpRequest(inviteCode), onError = onError, onSuccess = onSuccess)

suspend fun Api.signIn(
    transferCode: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<TokenResponse> = {},
) = post("sign/in", SignInRequest(transferCode), onError = onError, onSuccess = onSuccess)
