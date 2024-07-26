package com.queatz.db

fun Db.bots(person: String) = list(
    Bot::class,
    """
        for bot in @@collection
            filter bot.${f(Bot::creator)} == @person
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
