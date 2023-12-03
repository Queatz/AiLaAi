package app.ailaai.api

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

suspend fun Api.signInWithLink(
    link: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<TokenResponse> = {},
) = post("sign/in", SignInRequest(link = link), onError = onError, onSuccess = onSuccess)
