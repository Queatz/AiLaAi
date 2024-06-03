package app.ailaai.api


import com.queatz.db.PlatformConfig
import com.queatz.db.PlatformMeResponse

suspend fun Api.platformMe(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PlatformMeResponse> = {}
) = get("platform/me", onError = onError, onSuccess = onSuccess)

suspend fun Api.platform(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PlatformConfig> = {}
) = get("platform", onError = onError, onSuccess = onSuccess)

suspend fun Api.platform(
    platformConfig: PlatformConfig,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PlatformConfig> = {}
) = post("platform", platformConfig, onError = onError, onSuccess = onSuccess)
