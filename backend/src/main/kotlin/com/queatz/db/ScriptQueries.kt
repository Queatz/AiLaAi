package com.queatz.db

fun Db.scriptsOfPerson(person: String) = list(
    Script::class,
    """
        for script in @@collection
            filter script.${f(Script::person)} == @person
            sort script.${f(Script::createdAt)} desc
            return script
    """.trimIndent(),
    mapOf("person" to person)
)

fun Db.allScripts(offset: Int = 0, limit: Int = 20) = list(
    Script::class,
    """
        for script in @@collection
            limit @offset, @limit
            return script
    """.trimIndent(),
    mapOf(
        "offset" to offset,
        "limit" to limit
    )
)

fun Db.searchScripts(search: String, offset: Int = 0, limit: Int = 20) = list(
    Script::class,
    """
        for script in @@collection
            filter contains(lower(x.${f(Script::name)}), @search)
            sort script.${f(Script::createdAt)} desc
            limit @offset, @limit
            return script
    """.trimIndent(),
    mapOf(
        "search" to search.lowercase(),
        "offset" to offset,
        "limit" to limit
    )
)
