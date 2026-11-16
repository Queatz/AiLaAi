import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.Account

suspend fun Api.account(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Account> = {}
) = get("account", errorContext = "load account", onError = onError, onSuccess = onSuccess)
