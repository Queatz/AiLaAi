package com.queatz.db

fun Db.openGroupsOfPerson(person: String, offset: Int = 0, limit: Int = 20) = query(
    GroupExtended::class,
    """
        for group, groupMember in outbound @person graph `${Member::class.graph()}`
            filter groupMember.${f(Member::gone)} != true
                and groupMember.${f(Member::hide)} != true
                and group.${f(Group::open)} == true
            sort groupMember.${f(Member::seen)} desc
            limit @offset, @limit
            return ${groupExtended()}
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "offset" to offset,
        "limit" to limit,
    )
)

fun Db.subscribe(from: String, to: String) = one(
    Subscription::class,
    """
        upsert { _from: @from, _to: @to }
            insert { _from: @from, _to: @to, ${f(Subscription::createdAt)}: DATE_ISO8601(DATE_NOW()) }
            update { _from: @from, _to: @to }
            in @@collection
            return NEW || OLD
    """.trimIndent(),
    mapOf(
        "from" to from.asId(Person::class),
        "to" to to.asId(Person::class)
    )
)!!

fun Db.unsubscribe(from: String, to: String) = query(
    Subscription::class,
    """
        for x in `${Subscription::class.collection()}`
            filter x._from == @from
                and x._to == @to
            remove x in `${Subscription::class.collection()}`
    """.trimIndent(),
    mapOf(
        "from" to from.asId(Person::class),
        "to" to to.asId(Person::class)
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
        "from" to from.asId(Person::class),
        "to" to to.asId(Person::class)
    )
)

fun Db.subscribersOf(people: List<String>) = query(
    Person::class,
    """
        for subscriber in `${Person::class.collection()}`
            for subscription in `${Subscription::class.collection()}`
                filter subscription._from == subscriber._id
                    and subscription._to in @people
                return distinct subscriber
    """.trimIndent(),
    mapOf(
        "people" to people.map { it.asId(Person::class) }
    )
)

fun Db.friendsCount(person: String) = query(
    Int::class,
    """
            return count(
                for group in outbound @person graph `${Member::class.graph()}`
                    for friend, member in inbound group graph `${Member::class.graph()}`
                        filter friend.${f(Person::source)} != ${v(PersonSource.Web)}
                            and member.${f(Member::gone)} != true
                            and member._from != @person
                        return distinct friend
            )
        """,
    mapOf(
        "person" to person.asId(Person::class)
    )
).first()!!

fun Db.cardsCount(person: String) = query(
    Int::class,
    """
            return count(
                for card in `${Card::class.collection()}`
                    filter card.${f(Card::person)} == @person
                        and card.${f(Card::active)} == true
                    return distinct card
            )
        """,
    mapOf(
        "person" to person
    )
).first()!!

fun Db.storiesCount(person: String) = query(
    Int::class,
    """
            return count(
                for story in `${Story::class.collection()}`
                    filter story.${f(Story::person)} == @person
                        and story.${f(Story::published)} == true
                    return distinct story
            )
        """,
    mapOf(
        "person" to person
    )
).first()!!

fun Db.subscriberCount(person: String) = query(
    Int::class,
    """
            return count(
                for subscription in `${Subscription::class.collection()}`
                    filter subscription._to == @person
                    return distinct subscription
            )
        """,
    mapOf(
        "person" to person.asId(Person::class)
    )
).first()!!

/**
 * Find people matching @name that are not connected with @person sorted by distance from @geo.
 */
fun Db.peopleWithName(person: String, name: String, geo: List<Double>? = null, limit: Int = 20) = list(
    Person::class,
    """
        for x in @@collection
            filter lower(x.${f(Person::name)}) == @name
                and first(
                    for group, edge in outbound @person graph `${Member::class.graph()}`
                        filter edge.${f(Member::hide)} != true 
                            and edge.${f(Member::gone)} != true
                        for otherPerson, otherEdge in inbound group graph `${Member::class.graph()}`
                            filter otherEdge._to == group._id
                                and otherPerson._id == x._id
                                and otherEdge.${f(Member::gone)} != true
                            limit 1
                            return true
                ) != true
            let d = x.${f(Person::geo)} == null || @geo == null ? null : distance(x.${f(Person::geo)}[0], x.${f(Person::geo)}[1], @geo[0], @geo[1])
            sort d
            sort d == null
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "name" to name.lowercase(),
        "geo" to geo,
        "limit" to limit
    )
)

/**
 * @people The list of people to fetch
 */
fun Db.people(people: List<String>) = list(
    Person::class,
    """
        for x in @@collection
            filter x._key in @people
            return x
    """.trimIndent(),
    mapOf(
        "people" to people
    )
)

/**
 * @person the person to fetch a profile for
 */
fun Db.profile(person: String) = one(
    Profile::class,
    """
    upsert { ${f(Profile::person)}: @person }
        insert { ${f(Profile::person)}: @person, ${f(Profile::createdAt)}: DATE_ISO8601(DATE_NOW()) }
        update { }
        in @@collection
        return NEW || OLD
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)!!

/**
 * @url the url to fetch a profile for
 */
fun Db.profileByUrl(url: String) = one(
    Profile::class,
    """
    for x in @@collection
        filter x.${f(Profile::url)} == @url
        limit 1
        return x
    """.trimIndent(),
    mapOf(
        "url" to url
    )
)

/**
 * @person The current user
 */
fun Db.presenceOfPerson(person: String) = one(
    Presence::class,
    """
    upsert { ${f(Presence::person)}: @person }
        insert {
            ${f(Presence::person)}: @person,
            ${f(Presence::createdAt)}: DATE_ISO8601(DATE_NOW())
        }
        update {}
        in @@collection
        return NEW || OLD
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)!!

fun Db.groupExtended(groupVar: String = "group") = """{
    $groupVar,
    ${f(GroupExtended::members)}: (
        for person, member in inbound $groupVar graph `${Member::class.graph()}`
            filter member.${f(Member::gone)} != true
            sort member.${f(Member::seen)} desc
            return {
                person,
                member
            }
    ),
    ${f(GroupExtended::latestMessage)}: first(
        for message in `${Message::class.collection()}`
            filter message.${f(Message::group)} == $groupVar._key
            sort message.${f(Message::createdAt)} desc
            limit 1
            return message
    ),
    ${f(GroupExtended::cardCount)}: count(for groupCard in `${Card::class.collection()}` filter groupCard.${f(Card::active)} == true and groupCard.${f(Card::group)} == $groupVar._key return true)
}"""
