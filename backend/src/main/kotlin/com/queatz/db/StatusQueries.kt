package com.queatz.db

fun Db.statuses(person: String) = list(
    PersonStatus::class,
    """
        let friends = ${friends(includeSelf = true)}
        for friend in friends
            let status = first(
                for x in `${PersonStatus::class.collection()}`
                    filter x.${f(PersonStatus::person)} == friend._key
                    sort x.${f(PersonStatus::createdAt)} desc
                    limit 1
                    return x 
            )
            filter status.${f(PersonStatus::note)} != null or status.${f(PersonStatus::status)} != null
            return merge(
                status,
                {
                    ${f(PersonStatus::statusInfo)}: (status.${f(PersonStatus::status)} != null ? document('${Status::class.collection()}', status.${f(PersonStatus::status)}) : null)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class)
    )
)

fun Db.recentStatuses(person: String) = list(
    Status::class,
    """
        let friends = ${friends(includeSelf = true)}
        let results = (
            for friend in friends
                for status in `${PersonStatus::class.collection()}`
                    filter status.${f(PersonStatus::person)} == friend._key
                        and status.${f(PersonStatus::status)} != null
                    sort status.${f(PersonStatus::createdAt)} desc
                    return distinct document('${Status::class.collection()}', status.${f(PersonStatus::status)})
        )
        for result in results
            limit 20
            return result
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class)
    )
)

fun Db.friends(personVar: String = "@person", includeSelf: Boolean = false) = """
    (
        for group, edge in outbound $personVar graph `${Member::class.graph()}`
            filter edge.${f(Member::gone)} != true
            for friend, member in inbound group graph `${Member::class.graph()}`
                filter member.${f(Member::gone)} != true
                        ${if (includeSelf) "" else "and member._from != $personVar"}
                    return distinct friend
    )
""".trimIndent()
