package com.queatz.db

fun Db.call(group: String) = one(
    Call::class,
    """
        for x in @@collection
            filter x.${f(Call::group)} == @group
            return x
    """.trimIndent(),
    mapOf(
        "group" to group
    )
)
