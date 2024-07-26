package app.ailaai.api

import com.queatz.db.Bot
import com.queatz.db.BotData
import com.queatz.db.BotDetailsBody
import com.queatz.db.GroupBot
import io.ktor.http.HttpStatusCode

suspend fun Api.createGroupBot(
    group: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GroupBot>>,
) = get("/groups/$group/bots", onError = onError, onSuccess = onSuccess)

suspend fun Api.groupBots(
    group: String,
    groupBot: GroupBot,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GroupBot>,
) = post("/groups/$group/bots", groupBot, onError = onError, onSuccess = onSuccess)

suspend fun Api.bots(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Bot>>,
) = get("bots", onError = onError, onSuccess = onSuccess)

suspend fun Api.createBot(
    details: BotDetailsBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Bot>,
) = post("bots", details, onError = onError, onSuccess = onSuccess)

suspend fun Api.bot(
    bot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Bot>,
) = get("bots/$bot", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateBot(
    bot: String,
    update: Bot,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Bot>,
) = post("bots/$bot", update, onError = onError, onSuccess = onSuccess)

suspend fun Api.reloadBot(
    bot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Bot>,
) = post("bots/$bot/reload", onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteBot(
    bot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post("bots/$bot/delete", onError = onError, onSuccess = onSuccess)

suspend fun Api.botData(
    bot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<BotData>,
) = get("bots/$bot/data", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateBotData(
    bot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<BotData>,
) = post("bots/$bot/data", onError = onError, onSuccess = onSuccess)

suspend fun Api.groupBot(
    groupBot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GroupBot>,
) = get("/group-bots/$groupBot", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateGroupBot(
    groupBot: String,
    update: GroupBot,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GroupBot>,
) = post("/group-bots/$groupBot", update, onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteGroupBot(
    groupBot: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post("/group-bots/$groupBot/delete", onError = onError, onSuccess = onSuccess)
