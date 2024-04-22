package com.queatz.db

/**
 * @param from The ID of the person
 * @param to The ID of the entity
 * @param reaction The reaction
 * @param comment The person's comment, or null
 */
fun Db.react(from: String, to: String, reaction: String, comment: String?) = one(
    Reaction::class,
    """
        upsert { _from: @from, _to: @to, reaction: @reaction }
            insert { _from: @from, _to: @to, ${f(Reaction::reaction)}: @reaction, ${f(Reaction::comment)}: @comment, ${f(Reaction::createdAt)}: DATE_ISO8601(DATE_NOW()) }
            update { ${f(Reaction::comment)}: @comment }
            in @@collection
            return NEW || OLD
    """.trimIndent(),
    mapOf(
        "from" to from,
        "to" to to,
        "reaction" to reaction,
        "comment" to comment
    )
)

fun Db.unreact(from: String, to: String, reaction: String) = query(
    Reaction::class,
    """
        for reaction in `${Reaction::class.collection()}`
            filter reaction._from == @from
                and reaction._to == @to
                and reaction.${f(Reaction::reaction)} == @reaction
            remove reaction in `${Reaction::class.collection()}`
    """.trimIndent(),
    mapOf(
        "from" to from,
        "to" to to,
        "reaction" to reaction
    )
)

fun Db.reactions(person: String?, to: String) = """
    {
        "${f(ReactionSummary::all)}": ${allReactions(to)},
        "${f(ReactionSummary::mine)}": ${person?.let { myReactions(it, to) }}
    }
""".trimIndent()

fun Db.myReactions(person: String, to: String) = """
    (
        for reaction in ${Reaction::class.collection()}
            filter reaction._from == $person
                and reaction._to == $to
                return reaction
    )
""".trimIndent()

fun Db.allReactions(to: String) = """
    (
        for entity, reaction in inbound $to graph `${Reaction::class.graph()}`
            collect text = reaction.${f(Reaction::reaction)} with count into count
            sort count desc
            return { ${f(ReactionCount::reaction)}: text, ${f(ReactionCount::count)}: count }
    )
""".trimIndent()
