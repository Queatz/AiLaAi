package com.queatz.db

fun Db.call(group: String) = one(
    Call::class,
    """
        for x in @@collection
            filter x.${f(Call::group)} == @group
            return x
    """.trimIndent(),
    mapOf(
        "group" to group
    )
)

fun Db.activeCallOfGroup(group: String) = one(
    Call::class,
    """
        for x in @@collection
            filter x.${f(Call::group)} == @group
                and (x.${f(Call::ongoing)} == true || x.${f(Call::participants)} > 0)
            limit 1
            return x
    """.trimIndent(),
    mapOf(
        "group" to group
    )
)

fun Db.callByRoom(room: String) = one(
    Call::class,
    """
        for x in @@collection
            filter x.${f(Call::room)} == @room
            return x
    """.trimIndent(),
    mapOf(
        "room" to room
    )
)

fun Db.activeCallsOfPerson(person: String) = list(
    Call::class,
    """
        for group, edge in outbound @person graph `${Member::class.graph()}`
            filter edge.${f(Member::hide)} != true
                and edge.${f(Member::gone)} != true
            sort group.${f(Group::seen)} desc
            for x in @@collection
                filter x.${f(Call::group)} == group._key
                    and (
                        x.${f(Call::ongoing)} == true || x.${f(Call::participants)} > 0
                    )
                return x
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class)
    )
)
