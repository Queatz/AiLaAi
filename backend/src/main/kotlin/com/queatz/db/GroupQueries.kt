package com.queatz.db

import com.queatz.plugins.defaultNearbyMaxDistanceInMeters
import kotlinx.datetime.Instant

/**
 * @person The current user
 */
fun Db.groups(person: String) = query(
    GroupExtended::class,
    """
        for group, edge in outbound @person graph `${Member::class.graph()}`
            filter edge.${f(Member::hide)} != true
                and edge.${f(Member::gone)} != true
            sort group.${f(Group::seen)} desc
            return ${groupExtended()}
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class)
    )
)

/**
 * @person The current user
 */
fun Db.groupsPlain(person: String) = query(
    Group::class,
    """
        for group, edge in outbound @person graph `${Member::class.graph()}`
            filter edge.${f(Member::hide)} != true
                and edge.${f(Member::gone)} != true
            sort group.${f(Group::seen)} desc
            return group
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class)
    )
)

/**
 * @geo The geo to bias for
 * @search Search query
 */
fun Db.openGroups(
    person: String?,
    geo: List<Double>? = null,
    nearbyMaxDistance: Double = defaultNearbyMaxDistanceInMeters,
    search: String? = null,
    public: Boolean = false,
    offset: Int = 0,
    limit: Int = 20
) = query(
    GroupExtended::class,
    """
        for group in ${Group::class.collection()}
            filter group.${f(Group::open)} == true
                and (@search == null or contains(lower(group.${f(Group::name)}), @search) )
            let d = group.${f(Group::geo)} != null ? distance(@geo[0], @geo[1], group.${f(Group::geo)}[0], group.${f(Group::geo)}[1]) : ${groupGeo()}
            filter ${if (public) "(d != null and d <= @nearbyMaxDistance)" else isFriendGroup()}
            sort d == null, d, group.${f(Group::seen)} desc
            limit @offset, @limit
            return ${groupExtended()}
    """.trimIndent(),
    mapOf(
        "geo" to geo,
        "nearbyMaxDistance" to nearbyMaxDistance,
        "search" to search,
        "offset" to offset,
        "limit" to limit,
    ).let {
        if (!public) {
            it + mapOf("person" to person)
        } else {
            it
        }
    }
)

/**
 * Used in openGroups() only
 *
 * @return Nearest group member's geo, excluding the current user
 */
fun Db.groupGeo() = """first(
    for person, member in inbound group graph `${Member::class.graph()}`
        filter member.${f(Member::gone)} != true
            and person.${f(Person::geo)} != null
            // and person._id != @person
            and member.${f(Member::seen)} >= date_subtract(DATE_NOW(), 180, 'day') // Only include active members
        let memberDistance = distance(person.${f(Person::geo)}[0], person.${f(Person::geo)}[1], @geo[0], @geo[1])
        sort memberDistance == null, memberDistance, member.${f(Member::seen)} desc
        limit 1
        return memberDistance
)"""

/**
 * Used in openGroups() only
 *
 * @return true if `@person` has any friends in `group`
 */
fun Db.isFriendGroup() = """first(
for g, myMember in outbound @person graph `${Member::class.graph()}`
    filter myMember.${f(Member::gone)} != true
    for groupPerson, groupMember in inbound group graph `${Member::class.graph()}`
        filter groupMember.${f(Member::gone)} != true
        for friend, member in inbound g graph `${Member::class.graph()}`
            filter member.${f(Member::gone)} != true
                and groupMember._id == member._id
                and member._from != @person
            limit 1
            return true
) == true"""

/**
 * @person The current user
 * @group The group to fetch
 */
fun Db.group(person: String?, group: String) = query(
    GroupExtended::class,
    """
        for group in ${Group::class.collection()}
            filter group._key == @group
                and (group.${f(Group::open)} == true ${
        if (person != null) {
            """
                            or first(
                                for x, member in outbound @person graph `${Member::class.graph()}`
                                    filter x._key == @group
                                        and member.${f(Member::gone)} != true
                                    limit 1
                                    return true
                            ) == true
                        """.trimIndent()
        } else ""
    })
            limit 1
            return ${groupExtended()}
    """.trimIndent(),
    buildMap {
        if (person != null) {
            set("person", person.asId(Person::class))
        }
        set("group", group)
    }
).firstOrNull()

/**
 * @me The current user
 */
fun Db.hiddenGroups(me: String) = query(
    GroupExtended::class,
    """
        for group, edge in outbound @person graph `${Member::class.graph()}`
            filter edge.${f(Member::hide)} == true
                and edge.${f(Member::gone)} != true
            sort group.${f(Group::seen)} desc
            return ${groupExtended()}
    """.trimIndent(),
    mapOf(
        "person" to me.asId(Person::class)
    )
)

/**
 * @person The current user
 * @group The group to fetch
 */
fun Db.groups(person: String, groups: List<String>) = list(
    Group::class,
    """
        for x in outbound @person graph `${Member::class.graph()}`
            filter x._key in @groups
            return x
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "groups" to groups,
    )
)

/**
 * @person The person in the group
 * @group The group
 */
fun Db.member(person: String, group: String) = one(
    Member::class,
    """
        for x in @@collection
            filter x._from == @person
                and x._to == @group
                and x.${f(Member::gone)} != true
            return x
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "group" to group.asId(Group::class),
    )
)

/**
 * @people The list of all people in the group
 *
 * @return The most recently active group with all these people, or null
 */
fun Db.group(people: List<String>) = one(
    Group::class,
    """
        for x in @@collection
            let members = (
                for person, edge in inbound x graph `${Member::class.graph()}`
                    filter edge.${f(Member::gone)} != true
                    return edge._from
            )
            filter @people all in members
                and count(@people) == count(members)
            sort x.${f(Group::seen)} desc
            limit 1
            return x
    """.trimIndent(),
    mapOf(
        "people" to people.map { it.asId(Person::class) }
    )
)

/**
 * @people The current user
 * @exact Whether to return only groups with exactly these members
 *
 * @return All groups containing all the people
 */
fun Db.groupsWith(people: List<String>, exact: Boolean = false) = query(
    GroupExtended::class,
    """
        for group in `${Group::class.collection()}`
            let members = (
                for person, edge in inbound group graph `${Member::class.graph()}`
                    filter edge.${f(Member::gone)} != true
                    return edge._from
            )
            filter @people all in members
                ${if (exact) "and count(@people) == count(members)" else ""}
            sort count(members), group.${f(Group::seen)} desc
            return ${groupExtended()}
    """.trimIndent(),
    mapOf(
        "people" to people.map { it.asId(Person::class) }
    )
)

fun Db.messages(group: String, before: Instant? = null, limit: Int = 20) = list(
    Message::class,
    """
        for x in @@collection
            filter x.${f(Message::group)} == @group
            and (@before == null or x.${f(Message::createdAt)} <= @before)
            sort x.${f(Message::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "group" to group,
        "before" to before,
        "limit" to limit
    )
)
