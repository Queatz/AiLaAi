package com.queatz.db


fun Db.joinRequest(person: String, group: String) = one(
    JoinRequest::class,
    """
        for x in @@collection
            filter x.${f(JoinRequest::person)} == @person
                and x.${f(JoinRequest::group)} == @group
            return x
    """.trimIndent(),
    mapOf(
        "person" to person,
        "group" to group
    )
)

fun Db.myJoinRequests(person: String) = query(
    JoinRequestAndPerson::class,
    """
        for x in ${JoinRequest::class.collection()}
            filter x.${f(JoinRequest::person)} == @person
            return {
                ${f(JoinRequestAndPerson::joinRequest)}: x,
                ${f(JoinRequestAndPerson::person)}: first(for person in ${Person::class.collection()} filter person._key == x.${f(JoinRequest::person)} limit 1 return person)
            }
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

fun Db.joinRequests(person: String) = query(
    JoinRequestAndPerson::class,
    """
        for group, edge in outbound @person graph `${Member::class.graph()}`
            filter edge.${f(Member::gone)} != true
                and edge.${f(Member::host)} == true
            for x in ${JoinRequest::class.collection()}
                filter x.${f(JoinRequest::group)} == group._key
                return {
                    ${f(JoinRequestAndPerson::joinRequest)}: x,
                    ${f(JoinRequestAndPerson::person)}: first(for person in ${Person::class.collection()} filter person._key == x.${f(JoinRequest::person)} limit 1 return person)
                }
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class)
    )
)
