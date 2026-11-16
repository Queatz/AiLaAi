package app.ailaai.api

import com.queatz.db.Bot
import com.queatz.db.BotData
import com.queatz.db.BotDetailsBody
import com.queatz.db.GroupBot
import com.queatz.db.GroupBotExtended
import io.ktor.http.HttpStatusCode

suspend fun Api.groupBots(
    group: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GroupBotExtended>>,
) = get("groups/$group/bots", errorContext = "load bots", onError = onError, onSuccess = onSuccess)

suspend fun Api.createGroupBot(
    group: String,
    groupBot: GroupBot,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GroupBot>,
) = post("groups/$group/bots", groupBot, errorContext = "add bot", onError = onError, onSuccess = onSuccess)

suspend fun Api.bots(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Bot>>,
) = get("bots", errorContext = "load bots", onError = onError, onSuccess = onSuccess)

suspend fun Api.createBot(
    details: BotDetailsBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Bot>,
) = post("bots", details, errorContext = "create bot", onError = onError, onSuccess = onSuccess)

suspend fun Api.bot(
    bot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Bot>,
) = get("bots/$bot", errorContext = "load bot", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateBot(
    bot: String,
    update: Bot,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Bot>,
) = post("bots/$bot", update, errorContext = "update bot", onError = onError, onSuccess = onSuccess)

suspend fun Api.reloadBot(
    bot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Bot>,
) = post("bots/$bot/reload", errorContext = "reload bot", onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteBot(
    bot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post("bots/$bot/delete", errorContext = "delete bot", onError = onError, onSuccess = onSuccess)

suspend fun Api.botData(
    bot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<BotData>,
) = get("bots/$bot/data", errorContext = "load bot data", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateBotData(
    bot: String,
    botData: BotData,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<BotData>,
) = post("bots/$bot/data", botData, errorContext = "update bot data", onError = onError, onSuccess = onSuccess)

suspend fun Api.groupBot(
    groupBot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GroupBot>,
) = get("group-bots/$groupBot", errorContext = "load bot", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateGroupBot(
    groupBot: String,
    update: GroupBot,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GroupBot>,
) = post("group-bots/$groupBot", update, errorContext = "update bot", onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteGroupBot(
    groupBot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post("group-bots/$groupBot/delete", errorContext = "remove bot", onError = onError, onSuccess = onSuccess)
