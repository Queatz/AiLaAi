package com.queatz.db

import com.queatz.plugins.defaultNearbyMaxDistanceInMeters
import kotlinx.datetime.Instant

fun Db.recentCrashes(limit: Int = 50) = list(
    Crash::class,
    """
        for x in @@collection
            sort x.${f(Card::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "limit" to limit
    )
)

fun Db.recentReports(limit: Int = 50) = list(
    Report::class,
    """
        for x in @@collection
            sort x.${f(Card::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "limit" to limit
    )
)

fun Db.recentSearches(limit: Int = 50) = list(
    Search::class,
    """
        for x in @@collection
            sort x.${f(Search::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "limit" to limit
    )
)

fun Db.recentFeedback(limit: Int = 50) = list(
    AppFeedback::class,
    """
        for x in @@collection
            sort x.${f(AppFeedback::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "limit" to limit
    )
)

fun Db.activePeople(days: Int) = query(
        Int::class,
        """
            return count(
                for x in `${Person::class.collection()}`
                    filter x.${f(Person::seen)} != null
                        and x.${f(Person::seen)} >= date_subtract(DATE_NOW(), @days, 'day')
                    return true
            )
        """,
        mapOf(
            "days" to days
        )
    ).first()!!

fun Db.newPeople(days: Int) = query(
        Int::class,
        """
            return count(
                for x in `${Person::class.collection()}`
                    filter x.${f(Person::createdAt)} != null
                        and x.${f(Person::createdAt)} >= date_subtract(DATE_NOW(), @days, 'day')
                    return true
            )
        """,
    mapOf(
        "days" to days
    )
).first()!!

val Db.totalPeople
    get() = query(
        Int::class,
        """
            return count(`${Person::class.collection()}`)
        """
    ).first()!!

val Db.totalDraftCards
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Card::class.collection()}`
                    filter x.${f(Card::active)} != true
                    return true
            )
        """
    ).first()!!

val Db.totalPublishedCards
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Card::class.collection()}`
                    filter x.${f(Card::active)} == true
                    return true
            )
        """
    ).first()!!

val Db.totalDraftStories
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Story::class.collection()}`
                    filter x.${f(Story::published)} != true
                    return true
            )
        """
    ).first()!!

val Db.totalPublishedStories
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Story::class.collection()}`
                    filter x.${f(Story::published)} == true
                    return true
            )
        """
    ).first()!!

val Db.totalOpenGroups
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Group::class.collection()}`
                    filter x.${f(Group::open)} == true
                    return true
            )
        """
    ).first()!!

val Db.totalClosedGroups
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Group::class.collection()}`
                    filter x.${f(Group::open)} != true
                    return true
            )
        """
    ).first()!!

val Db.totalReminders
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Reminder::class.collection()}`
                    return true
            )
        """
    ).first()!!

/**
 * @code The code of the invite to fetch
 */
fun Db.invite(code: String) = one(
    Invite::class,
    """
        for x in @@collection
            filter x.${f(Invite::code)} == @code
            return x
    """.trimIndent(),
    mapOf(
        "code" to code
    )
)

/**
 * @token The token
 */
fun Db.linkDeviceToken(token: String) = one(
    LinkDeviceToken::class,
    """
        for x in @@collection
            filter x.${f(LinkDeviceToken::token)} == @token
            return x
    """.trimIndent(),
    mapOf(
        "token" to token
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
    members: (
        for person, member in inbound $groupVar graph `${Member::class.graph()}`
            filter member.${f(Member::gone)} != true
            sort member.${f(Member::seen)} desc
            return {
                person,
                member
            }
    ),
    latestMessage: first(
        for message in `${Message::class.collection()}`
            filter message.${f(Message::group)} == $groupVar._key
            sort message.${f(Message::createdAt)} desc
            limit 1
            return message
    ),
    cardCount: count(for groupCard in `${Card::class.collection()}` filter groupCard.${f(Card::active)} == true and groupCard.${f(Card::group)} == $groupVar._key return true)
}"""

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
            let d = ${groupGeo()}
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
            and member.${f(Member::seen)} >= date_subtract(DATE_NOW(), 7, 'day') // Only include active members
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
 * The current user
 */
fun Db.transferOfPerson(person: String) = one(
    Transfer::class,
    """
        for x in @@collection
            filter x.${f(Transfer::person)} == @person
            return x
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

/**
 * @code the code of the transfer to fetch
 */
// todo filter 5 min
fun Db.transferWithCode(code: String) = one(
    Transfer::class,
    """
        for x in @@collection
            filter x.${f(Transfer::code)} == @code
            limit 1
            return x
    """.trimIndent(),
    mapOf(
        "code" to code
    )
)

/**
 * @group The group to fetch devices for
 */
fun Db.memberDevices(group: String, onlyHosts: Boolean = false) = query(
    MemberDevice::class,
    """
        for member in `${Member::class.collection()}`
            filter member._to == @group and member.${f(Member::gone)} != true
                and (@onlyHosts != true or member.${f(Member::host)} == true)
            return {
                member,
                devices: (
                    for device in `${Device::class.collection()}`
                        filter device.${f(Device::person)} == member._from
                        return device
                )
            }
    """.trimIndent(),
    mapOf(
        "group" to group.asId(Group::class),
        "onlyHosts" to onlyHosts
    )
)
/**
 * @people The people to fetch devices for
 */
fun Db.peopleDevices(people: List<String>) = list(
    Device::class,
    """
        for device in @@collection
            filter device.${f(Device::person)} in @people
            return device
    """.trimIndent(),
    mapOf(
        "people" to people.map { it.asId(Person::class) }
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

fun Db.updateDevice(person: String, type: DeviceType, token: String) = one(
    Device::class,
        """
            upsert { ${f(Device::type)}: @type, ${f(Device::token)}: @token }
                insert { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Device::person)}: @person, ${f(Person::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Device::person)}: @person }
                in @@collection
                return NEW || OLD
        """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "type" to type,
        "token" to token
    )
)

fun Db.deleteDevice(type: DeviceType, token: String) = query(
    Device::class,
    """
        for x in ${Device::class.collection()}
            filter x.${f(Device::type)} == @type
                and x.${f(Device::token)} == @token
            remove x in ${Device::class.collection()}
    """.trimIndent(),
    mapOf(
        "type" to type,
        "token" to token
    )
)

fun Db.device(type: DeviceType, token: String) = one(
    Device::class,
        """
            upsert { ${f(Device::type)}: @type, ${f(Device::token)}: @token }
                insert { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Person::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { }
                in @@collection
                return NEW || OLD
        """,
    mapOf(
        "type" to type,
        "token" to token
    )
)!!

fun Db.withStickers(stickerPackVal: String) = """
    merge(
        $stickerPackVal,
        {
            ${f(StickerPack::stickers)}: (
                for sticker in ${Sticker::class.collection()}
                    filter sticker.${f(Sticker::pack)} == $stickerPackVal._key
                    sort sticker.${f(Sticker::name)}, sticker.${f(Sticker::createdAt)}
                    return sticker
                )
        }
    )
""".trimIndent()

fun Db.myStickerPacks(person: String) = list(
    StickerPack::class,
    """
        for pack in @@collection
            filter pack.${f(StickerPack::person)} == @person
            sort pack.${f(StickerPack::createdAt)} desc
            return ${withStickers("pack")}
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

fun Db.stickerPacks(person: String)  = list(
    StickerPack::class,
    """
        for pack, save in outbound @person graph `${StickerPackSave::class.graph()}`
            sort save.${f(StickerPackSave::createdAt)} desc
            return ${withStickers("pack")}
    """,
    mapOf(
        "person" to person.asId(Person::class)
    )
)

fun Db.stickerPackWithStickers(stickerPack: String) = one(
    StickerPack::class,
    """
        for pack in @@collection
            filter pack._key == @pack
            return ${withStickers("pack")}
    """,
    mapOf(
        "pack" to stickerPack
    )
)

fun Db.saveStickerPack(person: String, stickerPack: String) = one(
    StickerPackSave::class,
    """
            upsert { _from: @person, _to: @pack }
                insert { _from: @person, _to: @pack, ${f(StickerPackSave::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { _from: @person, _to: @pack }
                in @@collection
                return NEW || OLD
        """,
    mapOf(
        "person" to person.asId(Person::class),
        "pack" to stickerPack.asId(StickerPack::class)
    )
)

fun Db.unsaveStickerPack(person: String, stickerPack: String) = query(
    StickerPackSave::class,
    """
            for x in `${StickerPackSave::class.collection()}`
                filter x._from == @person
                    and x._to == @pack
                remove x in `${StickerPackSave::class.collection()}`
        """,
    mapOf(
        "person" to person.asId(Person::class),
        "pack" to stickerPack.asId(StickerPack::class)
    )
)

fun Db.stickers(stickerPack: String) = list(
    Sticker::class,
    """
        for x in @@collection
            filter x.${f(Sticker::pack)} == @pack
            return x
    """.trimIndent(),
    mapOf(
        "pack" to stickerPack
    )
)

