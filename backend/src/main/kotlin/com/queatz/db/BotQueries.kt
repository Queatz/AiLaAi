package com.queatz.db

fun Db.bots(person: String) = list(
    Bot::class,
    """
        for bot in @@collection
            filter bot.${f(Bot::creator)} == @person
                or bot.${f(Bot::open)} == true
            return bot
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

fun Db.groupBotsOfGroup(group: String) = list(
    GroupBot::class,
    """
        for groupBot in @@collection
            filter groupBot.${f(GroupBot::group)} == @group
            return groupBot
    """.trimIndent(),
    mapOf(
        "group" to group
    )
)

fun Db.groupBotsOfBot(bot: String) = list(
    GroupBot::class,
    """
        for groupBot in @@collection
            filter groupBot.${f(GroupBot::bot)} == @bot
            return groupBot
    """.trimIndent(),
    mapOf(
        "bot" to bot
    )
)

fun Db.groupBots(group: String) = query(
    GroupBotExtended::class,
    """
        for groupBot in `${GroupBot::class.collection()}`
            filter groupBot.${f(GroupBot::group)} == @group
            return {
                ${f(GroupBotExtended::bot)}: document(${Bot::class.collection()}, groupBot.${f(GroupBot::bot)}),
                ${f(GroupBotExtended::groupBot)}: groupBot
            }
    """.trimIndent(),
    mapOf(
        "group" to group
    )
)

fun Db.botData(bot: String) = one(
    BotData::class,
    """
        for botData in @@collection
            filter botData.${f(BotData::bot)} == @bot
            return botData
    """.trimIndent(),
    mapOf(
        "bot" to bot
    )
)

fun Db.groupBotData(groupBot: String) = one(
    GroupBotData::class,
    """
        for groupBotData in @@collection
            filter groupBotData.${f(GroupBotData::groupBot)} == @groupBot
            return groupBotData
    """.trimIndent(),
    mapOf(
        "groupBot" to groupBot
    )
)

fun Db.groupBotByWebhook(webhook: String) = one(
    GroupBot::class,
    """
        for groupBotData in `${GroupBotData::class.collection()}`
            filter groupBotData.${f(GroupBotData::webhook)} == @webhook
            return document(${GroupBot::class.collection()}, groupBotData.${f(GroupBotData::groupBot)})
    """.trimIndent(),
    mapOf(
        "webhook" to webhook
    )
)
