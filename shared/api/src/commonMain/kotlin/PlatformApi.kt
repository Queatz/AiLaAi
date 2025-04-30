package app.ailaai.api


import com.queatz.db.Account
import com.queatz.db.Person
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
) = post(
    "platform/accounts/$account/points",
    PlatformAccountsPointsBody(add = points),
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.platform(
    platformConfig: PlatformConfig,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PlatformConfig> = {}
) = post("platform", platformConfig, onError = onError, onSuccess = onSuccess)

suspend fun Api.platformAccountsByPoints(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Account>> = {}
) = get("platform/points/accounts", onError = onError, onSuccess = onSuccess)

suspend fun Api.platformLatestMembers(
    offset: Int = 0,
    limit: Int = 20,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Person>> = {}
) = get(
    url = "platform/members",
    parameters = mapOf(
        "offset" to offset.toString(),
        "limit" to limit.toString(),
    ),
    onError = onError,
    onSuccess = onSuccess
)
