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
