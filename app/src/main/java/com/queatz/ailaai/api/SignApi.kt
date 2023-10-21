package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.SuccessBlock
import com.queatz.db.SignInRequest
import com.queatz.db.SignUpRequest
import com.queatz.db.TokenResponse

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
