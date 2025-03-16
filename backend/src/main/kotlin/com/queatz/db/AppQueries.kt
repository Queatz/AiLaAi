package com.queatz.db

fun Db.apps(person: String?) = list(
    Bot::class,
    """
        for app in @@collection
            filter app.${f(App::creator)} == @person
                or app.${f(App::open)} == true
            return app
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)
