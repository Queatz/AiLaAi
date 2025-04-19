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
            let person = document(${Person::class.collection()}, script.${f(Script::person)})
            sort script.${f(Script::createdAt)} desc
            limit @offset, @limit
            return merge(script, {
                ${f(Script::author)}: {
                    id: person._key,
                    ${f(Person::name)}: person.${f(Person::name)}
                }
            })
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
                or (is_array(script.${f(Script::categories)}) and first(for c in (script.${f(Card::categories)} || []) filter contains(lower(c), @search) return true) == true)
            let person = document(${Person::class.collection()}, script.${f(Script::person)})
            sort script.${f(Script::createdAt)} desc
            limit @offset, @limit
            return merge(script, {
                ${f(Script::author)}: {
                    id: person._key,
                    ${f(Person::name)}: person.${f(Person::name)}
                }
            })
    """.trimIndent(),
    mapOf(
        "search" to search.lowercase(),
        "offset" to offset,
        "limit" to limit
    )
)
