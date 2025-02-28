package com.queatz.db

fun Db.ratings(
    person: String,
    offset: Int,
    limit: Int,
    descending: Boolean = true
) = list(
    Rating::class,
    """
        for x in @@collection
            filter x._from == @person
            sort x.${f(Rating::rating)} ${if (descending) "desc" else "asc"}
            limit @offset, @limit
            return x
        
    """,
    mapOf(
        "person" to person.asId(Person::class),
        "offset" to offset,
        "limit" to limit
    )
)

fun Db.rating(
    personId: String,
    entityId: String
) = one(
    Rating::class,
    """
        for x in @@collection
            filter x._from == @person
                and x._to == @entity
            limit 1
            return x
        
    """,
    mapOf(
        "person" to personId,
        "entity" to entityId,
    )
)

fun Db.setRating(
    personId: String,
    entityId: String,
    rating: Int?
) = one(
    Rating::class,
    """
        upsert { _from: @person, _to: @entity }
            insert { _from: @person, _to: @entity, ${f(Rating::rating)}: @rating, ${f(Rating::createdAt)}: DATE_ISO8601(DATE_NOW()) }
            update { _from: @person, _to: @entity, ${f(Rating::rating)}: @rating }
            in @@collection
            return NEW || OLD
    """.trimIndent(),
    mapOf(
        "person" to personId,
        "entity" to entityId,
        "rating" to rating,
    )
)
