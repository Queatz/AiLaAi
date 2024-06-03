package com.queatz.db

val Db.platformConfig get() = one(
    PlatformConfig::class,
    """
        for x in @@collection limit 1 return x
    """.trimIndent()
)
