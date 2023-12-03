package com.queatz.db

fun Db.widget(person: String, id: String) = one(
    Widget::class,
    """
        for x in @@collection
            filter x._key == @id
                and x.${f(Widget::person)} == @person
            return x
    """.trimIndent(),
    mapOf(
        "person" to person,
        "id" to id,
    )
)
