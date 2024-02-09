import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.Trade
import com.queatz.db.TradeExtended
import com.queatz.db.TradeItem

suspend fun Api.createTrade(
    trade: Trade,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Trade> = {}
) = post("trades", trade, onError = onError, onSuccess = onSuccess)

suspend fun Api.trades(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<TradeExtended>> = {}
) = get("trades", onError = onError, onSuccess = onSuccess)

suspend fun Api.trade(
    tradeId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<TradeExtended> = {}
) = get("trades/$tradeId", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateTrade(
    tradeId: String,
    trade: Trade,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<TradeExtended> = {}
) = post("trades/$tradeId", trade, onError = onError, onSuccess = onSuccess)

suspend fun Api.updateTradeItems(
    tradeId: String,
    tradeItems: List<TradeItem>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<TradeExtended> = {}
) = post("trades/$tradeId/items", tradeItems, onError = onError, onSuccess = onSuccess)

suspend fun Api.confirmTrade(
    tradeId: String,
    trade: Trade,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<TradeExtended> = {}
) = post("trades/$tradeId/confirm", trade, onError = onError, onSuccess = onSuccess)

suspend fun Api.unconfirmTrade(
    tradeId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<TradeExtended> = {}
) = post("trades/$tradeId/unconfirm", onError = onError, onSuccess = onSuccess)

suspend fun Api.cancelTrade(
    tradeId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<TradeExtended> = {}
) = post("trades/$tradeId/cancel", onError = onError, onSuccess = onSuccess)
