package com.queatz.db

/**
 * @person The current user
 * @script The script to pin
 */
fun Db.pinScript(person: String, script: String) = one(
    ScriptPin::class,
    """
        upsert { ${f(ScriptPin::person)}: @person, ${f(ScriptPin::script)}: @script }
            insert { ${f(ScriptPin::person)}: @person, ${f(ScriptPin::script)}: @script, ${f(ScriptPin::createdAt)}: DATE_ISO8601(DATE_NOW()) }
            update { ${f(ScriptPin::person)}: @person, ${f(ScriptPin::script)}: @script}
            in @@collection
            return NEW || OLD
    """,
    mapOf(
        "person" to person,
        "script" to script
    )
)

/**
 * @person The current user
 * @script The script to unpin
 */
fun Db.unpinScript(person: String, script: String) = query(
    ScriptPin::class,
    """
        for x in `${ScriptPin::class.collection()}`
            filter x.${f(ScriptPin::person)} == @person
                and x.${f(ScriptPin::script)} == @script
            remove x in `${ScriptPin::class.collection()}`
    """,
    mapOf(
        "person" to person,
        "script" to script
    )
)

/**
 * @person The current user
 * @return List of script IDs that are pinned by the user
 */
fun Db.pinnedScripts(person: String) = list(
    ScriptPin::class,
    """
        for pin in @@collection
            filter pin.${f(ScriptPin::person)} == @person
            return pin
    """.trimIndent(),
    mapOf("person" to person)
)

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
            let pin = first(
                for pin in ${ScriptPin::class.collection()}
                    filter pin.${f(ScriptPin::person)} == @person
                        and pin.${f(ScriptPin::script)} == script._key
                    return true
            )
            sort pin desc, script.${f(Script::createdAt)} desc
            return merge(script, { ${f(Script::pin)}: pin == true })
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
                or contains(lower(script.${f(Script::description)}), @search)
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
