package com.queatz.db

fun Db.personChips(
    person: String,
) = list(
    PersonChip::class,
    """
        for x in @@collection
            filter x.${f(PersonChip::person)} == @person
            sort (x.${f(PersonChip::isAbove)} != false ? 0 : 1) asc,
                x.${f(PersonChip::order)} asc,
                x.${f(PersonChip::createdAt)} asc
            return x
    """.trimIndent(),
    mapOf(
        "person" to person.asKey(),
    ),
)

fun Db.personChipsExtended(
    person: String,
) = query(
    PersonChipExtended::class,
    """
        for x in `${PersonChip::class.collection()}`
            filter x.${f(PersonChip::person)} == @person
            sort (x.${f(PersonChip::isAbove)} != false ? 0 : 1) asc,
                x.${f(PersonChip::order)} asc,
                x.${f(PersonChip::createdAt)} asc
            let feed = x.${f(PersonChip::feed)} != null
                ? document(concat('${ChipFeed::class.collection()}/', x.${f(PersonChip::feed)}))
                : null
            return {
                chip: unset(x, '${f(PersonChip::feed)}'),
                value: feed.${f(ChipFeed::value)},
                syncedAt: feed.${f(ChipFeed::syncedAt)}
            }
    """.trimIndent(),
    mapOf(
        "person" to person.asKey(),
    ),
)

fun Db.personChipExtended(
    person: String,
    chipKey: String,
) = query(
    PersonChipExtended::class,
    """
        for x in `${PersonChip::class.collection()}`
            filter x._key == @chipKey and x.${f(PersonChip::person)} == @person
            let feed = x.${f(PersonChip::feed)} != null
                ? document(concat('${ChipFeed::class.collection()}/', x.${f(PersonChip::feed)}))
                : null
            return {
                chip: unset(x, '${f(PersonChip::feed)}'),
                value: feed.${f(ChipFeed::value)},
                syncedAt: feed.${f(ChipFeed::syncedAt)}
            }
    """.trimIndent(),
    mapOf(
        "person" to person.asKey(),
        "chipKey" to chipKey.asKey(),
    ),
).firstOrNull()

fun Db.upsertChipFeed(
    url: String,
) = one(
    ChipFeed::class,
    """
        upsert { ${f(ChipFeed::url)}: @url }
            insert {
                ${f(ChipFeed::url)}: @url,
                ${f(ChipFeed::createdAt)}: DATE_ISO8601(DATE_NOW())
            }
            update {}
            in @@collection
            return NEW
    """.trimIndent(),
    mapOf(
        "url" to url,
    ),
)!!

fun Db.chipFeedByUrl(
    url: String,
) = one(
    ChipFeed::class,
    """
        for x in @@collection
            filter x.${f(ChipFeed::url)} == @url
            limit 1
            return x
    """.trimIndent(),
    mapOf(
        "url" to url,
    ),
)

fun Db.chipSubscribers(
    feedKey: String,
): List<String> = query(
    String::class,
    """
        for x in `${PersonChip::class.collection()}`
            filter x.${f(PersonChip::feed)} == @feedKey
            collect person = x.${f(PersonChip::person)}
            filter person != null
            return person
    """.trimIndent(),
    mapOf(
        "feedKey" to feedKey.asKey(),
    ),
)

fun Db.countChipSubscribers(
    feedKey: String,
): Int = query(
    Int::class,
    """
        return length(
            for x in `${PersonChip::class.collection()}`
                filter x.${f(PersonChip::feed)} == @feedKey
                return 1
        )
    """.trimIndent(),
    mapOf(
        "feedKey" to feedKey.asKey(),
    ),
).firstOrNull() ?: 0
