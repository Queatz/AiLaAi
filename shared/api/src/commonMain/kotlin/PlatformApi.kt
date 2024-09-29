package app.ailaai.api


import com.queatz.db.PlatformAccountsPointsBody
import com.queatz.db.PlatformConfig
import com.queatz.db.PlatformMeResponse
import io.ktor.http.HttpStatusCode

suspend fun Api.platformMe(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PlatformMeResponse> = {}
) = get("platform/me", onError = onError, onSuccess = onSuccess)

suspend fun Api.platform(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PlatformConfig> = {}
) = get("platform", onError = onError, onSuccess = onSuccess)

suspend fun Api.platformAddPoints(
    account: String,
    points: Int,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("platform/accounts/$account/points", PlatformAccountsPointsBody(add = points), onError = onError, onSuccess = onSuccess)

suspend fun Api.platform(
    platformConfig: PlatformConfig,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PlatformConfig> = {}
) = post("platform", platformConfig, onError = onError, onSuccess = onSuccess)
