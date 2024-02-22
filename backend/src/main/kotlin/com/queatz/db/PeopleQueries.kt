package com.queatz.db

fun Db.openGroupsOfPerson(person: String, offset: Int = 0, limit: Int = 20) = query(
    GroupExtended::class,
    """
        for group, x in outbound @person graph `${Member::class.graph()}`
            filter x.${f(Member::gone)} != true
                and group.${f(Group::open)} == true
            sort x.${f(Member::seen)} desc
            limit @offset, @limit
            return ${groupExtended()}
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "offset" to offset,
        "limit" to limit,
    )
)

fun Db.subscribe(from: String, to: String) = query(
    Subscription::class,
    """
        upsert { _from: @from, _to: @to }
            insert { _from: @from, _to: @to, ${f(Subscription::createdAt)}: DATE_ISO8601(DATE_NOW()) }
            update { _from: @from, _to: @to }
            in `${Subscription::class.collection()}`
            return NEW || OLD
    """.trimIndent(),
    mapOf(
        "from" to from,
        "to" to to
    )
).first()!!

fun Db.unsubscribe(from: String, to: String) = query(
    Subscription::class,
    """
        upsert { _from: @from, _to: @to }
            insert { _from: @from, _to: @to, ${f(Subscription::createdAt)}: DATE_ISO8601(DATE_NOW()) }
            update { _from: @from, _to: @to }
            remove x in `${Subscription::class.collection()}`
    """.trimIndent(),
    mapOf(
        "from" to from,
        "to" to to
    )
)

fun Db.subscription(from: String, to: String) = one(
    Subscription::class,
    """
        for x in @@collection
            filter x._from == @from
                and x._to == @to
            return x
    """.trimIndent(),
    mapOf(
        "from" to from,
        "to" to to
    )
)
