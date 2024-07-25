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
