package com.queatz.db

/**
 * @token The token
 */
fun Db.linkDeviceToken(token: String) = one(
    LinkDeviceToken::class,
    """
        for x in @@collection
            filter x.${f(LinkDeviceToken::token)} == @token
            return x
    """.trimIndent(),
    mapOf(
        "token" to token
    )
)
