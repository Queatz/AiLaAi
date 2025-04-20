package com.queatz.db

fun Db.prompts(
    person: String,
    context: PromptContext?,
    offset: Int,
    limit: Int,
) = list(
    Prompt::class,
    """
        for x in @@collection
            filter x.${f(Prompt::person)} == @person
                and x.${f(Prompt::context)} == @context
            sort x.${f(Prompt::lastUsed)} desc
            limit @offset, @limit
            return x
        
    """,
    mapOf(
        "person" to person,
        "context" to context,
        "offset" to offset,
        "limit" to limit,
    )
)

fun Db.addPrompt(
    person: String,
    prompt: String,
    context: PromptContext? = null
) = one(
    Prompt::class,
    """
        upsert { ${f(Prompt::person)}: @person, ${f(Prompt::prompt)}: @prompt, ${f(Prompt::context)}: @context }
            insert { ${f(Prompt::person)}: @person, ${f(Prompt::prompt)}: @prompt, ${f(Prompt::context)}: @context, ${f(Prompt::lastUsed)}: DATE_ISO8601(DATE_NOW()), ${f(Prompt::createdAt)}: DATE_ISO8601(DATE_NOW()) }
            update { ${f(Prompt::person)}: @person, ${f(Prompt::prompt)}: @prompt, ${f(Prompt::context)}: @context, ${f(Prompt::lastUsed)}: DATE_ISO8601(DATE_NOW()) }
            in @@collection
            return NEW || OLD
    """.trimIndent(),
    mapOf(
        "person" to person,
        "prompt" to prompt,
        "context" to context,
    )
)
