package com.queatz.db

import kotlin.time.Instant

/**
 * @person The current user
 */
fun Db.cardsOfPerson(person: String) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::person)} == @person
            sort x.${f(Card::createdAt)} desc
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

/**
 * @person The person to search
 * @search The search string
 */
fun Db.activeCardsOfPerson(person: String, search: String?, offset: Int = 0, limit: Int = 20) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::person)} == @person
                and x.${f(Card::active)} == true
                and (@search == null or contains(lower(x.${f(Card::name)}), @search) or contains(lower(x.${f(Card::location)}), @search) or contains(lower(x.${f(Card::conversation)}), @search))
            sort x.${f(Card::createdAt)} desc
            limit @offset, @limit
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person,
        "search" to search?.lowercase(),
        "offset" to offset,
        "limit" to limit
    )
)

/**
 * @person The person to fetch equipped cards for
 */
fun Db.equippedCardsOfPerson(person: String, me: String?) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::person)} == @person
                and x.${f(Card::equipped)} == true
                and x.${f(Card::active)} == true
            sort x.${f(Card::createdAt)} desc
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter ((@me != null && card.${f(Card::person)} == @me) or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person,
        "me" to me,
    )
)

/**
 * @me The current user
 */
fun Db.cardsOfGroup(me: String?, group: String) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::group)} == @group
                and (x.${f(Card::person)} == @me or x.${f(Card::active)} == true)
            sort x.${f(Card::createdAt)} desc
            return merge(
                x,
                {
                    ${f(Card::cardCount)}: count(for card in @@collection filter ((@me != null && card.${f(Card::person)} == @me) or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "me" to me,
        "group" to group,
    )
)

/**
 * @person The current user
 */
fun Db.collaborationsOfPerson(person: String) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::person)} == @person
                or @person in x.${f(Card::collaborators)}
            sort x.${f(Card::createdAt)} desc
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

/**
 * @person The current user
 * @search Optionally filter cards
 */
fun Db.savedCardsOfPerson(person: String, search: String? = null, offset: Int = 0, limit: Int = 20) = query(
    SaveAndCard::class,
    """
        for save in `${Save::class.collection()}`
            for x in `${Card::class.collection()}`
            filter x._key == save.${f(Save::card)}
                and save.${f(Save::person)} == @person
                and (@search == null or contains(lower(x.${f(Card::name)}), @search) or contains(lower(x.${f(Card::location)}), @search) or contains(lower(x.${f(Card::conversation)}), @search))
            sort save.${f(Save::createdAt)} desc
            limit @offset, @limit
            return {
                save: save,
                card: merge(
                    x,
                    {
                        cardCount: count(for card in `${Card::class.collection()}` filter card.${f(Card::active)} == true && card.${f(Card::parent)} == x._key return card)
                    }
                )
            }
    """.trimIndent(),
    mapOf(
        "person" to person,
        "search" to search?.lowercase(),
        "offset" to offset,
        "limit" to limit
    )
)

/**
 * @person The current user
 * @card The card to save
 */
fun Db.saveCard(person: String, card: String) = one(
    Save::class,
    """
            upsert { ${f(Save::person)}: @person, ${f(Save::card)}: @card }
                insert { ${f(Save::person)}: @person, ${f(Save::card)}: @card, ${f(Save::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { ${f(Save::person)}: @person, ${f(Save::card)}: @card}
                in @@collection
                return NEW || OLD
        """,
    mapOf(
        "person" to person,
        "card" to card
    )
)

/**
 * @person The current user
 * @card The card to unsave
 */
fun Db.unsaveCard(person: String, card: String) = query(
    Save::class,
    """
            for x in `${Save::class.collection()}`
                filter x.${f(Save::person)} == @person
                    and x.${f(Save::card)} == @card
                remove x in `${Save::class.collection()}`
        """,
    mapOf(
        "person" to person,
        "card" to card
    )
)

/**
 * @person The current user
 * @geo The current user's geolocation
 */
fun Db.updateEquippedCards(person: String, geo: List<Double>) = query(
    Card::class,
    """
        for x in `${Card::class.collection()}`
            filter x.${f(Card::person)} == @person
                and x.${f(Card::equipped)} == true
            update { _key: x._key, ${f(Card::geo)}: @geo } in `${Card::class.collection()}`
    """.trimIndent(),
    mapOf(
        "geo" to geo,
        "person" to person
    )
)

/**
 * @person The current user
 * @geo The geolocation bias
 * @search Optionally filter cards
 * @nearbyMaxDistance Optionally include cards nearby that may not be of the current user's friends
 * @offset Page offset
 * @limit Page size
 */
fun Db.explore(
    geo: List<Double>,
    altitude: Double? = null,
    search: String? = null,
    paid: Boolean? = null,
    nearbyMaxDistance: Double = 0.0,
    offset: Int = 0,
    limit: Int = 20
) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::active)} == true
                ${if (paid != null) "and x.${f(Card::pay)} ${if (paid) "!=" else "=="} null" else ""}
                and x.${f(Card::offline)} != true
                and (
                    @search == null 
                        or contains(lower(x.${f(Card::name)}), @search)
                        or contains(lower(x.${f(Card::location)}), @search)
                        or contains(lower(x.${f(Card::conversation)}), @search)
                        or (is_array(x.${f(Card::categories)}) and first(for c in (x.${f(Card::categories)} || []) filter contains(lower(c), @search) return true) == true)
                )
            for person in ${Person::class.collection()}
                filter person._key == x.${f(Card::person)}
                
                let geo = x.${f(Card::geo)} == null ? person.${f(Person::geo)} : x.${f(Card::geo)}
                let d = geo == null ? null : distance(geo[0], geo[1], @geo[0], @geo[1])
                filter d != null and d <= @nearbyMaxDistance
                sort x.${f(Card::level)} desc,
                    ${if (altitude == null) "x.${f(Card::size)} desc" else "abs((x.${f(Card::size)} || 0) - @altitude)"},
                    d == null,
                    d,
                    x.${f(Card::createdAt)} desc
                limit @offset, @limit
                return merge(
                    x,
                    {
                        cardCount: count(for card in @@collection filter card.${f(Card::active)} == true && card.${f(Card::parent)} == x._key return true)
                    }
                )
    """.trimIndent(),
    buildMap {
        put("geo", geo)
        if (altitude != null) {
            put("altitude", altitude)
        }
        put("search", search?.trim()?.lowercase())
        put("nearbyMaxDistance", nearbyMaxDistance)
        put("offset", offset)
        put("limit", limit)
    }
)

fun Db.isFriendCard() = """first(
for group, myMember in outbound @person graph `${Member::class.graph()}`
    filter myMember.${f(Member::gone)} != true
    for friend, member in inbound group graph `${Member::class.graph()}`
        filter member.${f(Member::gone)} != true
            and friend._key == x.${f(Card::person)}
        limit 1
        return true
) == true"""

/**
 * @card The card
 * @person The current user
 */
fun Db.cardsOfCard(card: String, person: String?) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::parent)} == @card
                and (x.${f(Card::active)} == true or x.${f(Card::person)} == @person)
           sort x.${f(Card::name)} asc
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "card" to card,
        "person" to person
    )
)

/**
 * @card The card to fetch all cards off
 */
fun Db.allCardsOfCard(card: String) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::parent)} == @card
            return x
    """.trimIndent(),
    mapOf(
        "card" to card
    )
)

fun Db.cardVisits(card: String, since: Instant) = list(
    CardVisit::class,
    """
        for visit in @@collection
            filter visit.${f(CardVisit::card)} == @card
                and visit.${f(CardVisit::createdAt)} >= @since
            sort visit.${f(CardVisit::createdAt)} desc
            return visit
    """.trimIndent(),
    mapOf(
        "card" to card,
        "since" to since
    )
)

/**
 * @url The card url
 */
fun Db.cardByUrl(url: String) = one(
    Card::class,
    """
        for x in @@collection
            filter x._key == @url
                or lower(x.${f(Card::url)}) == @url
            limit 1
            return x
    """.trimIndent(),
    mapOf(
        "url" to url.lowercase()
    )
)
