package com.queatz.db

fun Db.scriptData(script: String) = one(
    ScriptData::class,
    """
        for scriptData in @@collection
            filter scriptData.${f(ScriptData::script)} == @script
            return scriptData
    """,
    mapOf("script" to script)
)

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
            sort script.${f(Script::createdAt)} desc
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
            filter contains(lower(script.${f(Script::name)}), @search)
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
