package com.queatz.db

import kotlinx.datetime.Instant

fun Db.storyExtended(person: String?, storyVal: String) = """
    merge(
        $storyVal,
        {
            ${f(Story::authors)}: (
                for author in ${Person::class.collection()}
                    filter author._key == $storyVal.${f(Story::person)}
                    return author
            ),
            ${f(Story::reactions)}: ${reactions(person?.let { "\"$it\"" }, "$storyVal._id")}
        }
    )
""".trimIndent()

/**
 * @story The story id
 */
fun Db.story(person: String?, story: String) = one(
    Story::class,
    """
        for x in @@collection
            filter x._key == @story
            limit 1
            return ${storyExtended(person, "x")}
    """.trimIndent(),
    mapOf(
        "story" to story
    )
)

/**
 * @story The story id
 */
fun Db.storyByUrl(person: String?, url: String) = one(
    Story::class,
    """
        for x in @@collection
            filter x._key == @url
                or x.${f(Story::url)} == @url
            limit 1
            return ${storyExtended(person, "x")}
    """.trimIndent(),
    mapOf(
        "url" to url
    )
)

/**
 * @person The current user
 * @story The story id
 */
fun Db.storyDraft(person: String, story: String) = one(
    StoryDraft::class,
    """
        for x in @@collection
            filter x.${f(StoryDraft::story)} == @story
                and first(
                    for story in ${Story::class.collection()}
                        filter story._key == x.${f(StoryDraft::story)}
                        return story
                    ).${f(Story::person)} == @person
            return x
    """.trimIndent(),
    mapOf(
        "person" to person,
        "story" to story
    )
)

/**
 * @person The current user
 */
fun Db.storiesOfPerson(person: String, published: Boolean? = null) = list(
    Story::class,
    """
        for x in @@collection
            filter x.${f(Story::person)} == @person
            ${if (published != null) "and x.${f(Story::published)} == $published" else ""}
            sort x.${f(Story::createdAt)} desc
            return ${storyExtended(person.asId(Person::class), "x")}
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

/**
 * @geo The geolocation bias
 * @public Local or friends stories
 * @person The current user
 */
fun Db.stories(
    geo: List<Double>,
    person: String,
    nearbyMaxDistance: Double,
    offset: Int,
    limit: Int,
    public: Boolean
) = list(
    Story::class,
    """
        for x in @@collection
            filter x.${f(Story::published)} == true
            let d = x.${f(Story::geo)} == null ? null : distance(x.${f(Story::geo)}[0], x.${f(Story::geo)}[1], @geo[0], @geo[1])
            filter (
                @public ? (d != null and d <= @nearbyMaxDistance) : (x.${f(Story::person)} == @personKey or first(
                    for group, myMember in outbound @person graph `${Member::class.graph()}`
                        filter myMember.${f(Member::gone)} != true
                        for friend, member in inbound group graph `${Member::class.graph()}`
                            filter member.${f(Member::gone)} != true
                                and friend._key == x.${f(Story::person)}
                            limit 1
                            return true
                ) == true)
            )
            sort x.${f(Story::publishDate)} desc, x.${f(Story::createdAt)} desc
            limit @offset, @limit
            return ${storyExtended(person.asId(Person::class), "x")}
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "personKey" to person,
        "geo" to geo,
        "nearbyMaxDistance" to nearbyMaxDistance,
        "offset" to offset,
        "limit" to limit,
        "public" to public
    )
)

/**
 * Combines local and friends stories
 *
 * @geo The geolocation bias
 * @person The current user
 */
fun Db.stories(
    geo: List<Double>,
    person: String,
    nearbyMaxDistance: Double,
    offset: Int,
    limit: Int
) = list(
    Story::class,
    """
        for x in @@collection
            filter x.${f(Story::published)} == true
            let d = x.${f(Story::geo)} == null ? null : distance(x.${f(Story::geo)}[0], x.${f(Story::geo)}[1], @geo[0], @geo[1])
            filter (
                (d != null and d <= @nearbyMaxDistance) or (x.${f(Story::person)} == @personKey or first(
                    for group, myMember in outbound @person graph `${Member::class.graph()}`
                        filter myMember.${f(Member::gone)} != true
                        for friend, member in inbound group graph `${Member::class.graph()}`
                            filter member.${f(Member::gone)} != true
                                and friend._key == x.${f(Story::person)}
                            limit 1
                            return true
                ) == true)
            )
            sort x.${f(Story::publishDate)} desc, x.${f(Story::createdAt)} desc
            limit @offset, @limit
            return ${storyExtended(person.asId(Person::class), "x")}
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "personKey" to person,
        "geo" to geo,
        "nearbyMaxDistance" to nearbyMaxDistance,
        "offset" to offset,
        "limit" to limit
    )
)

/**
 * Public
 *
 * @geo The geolocation bias
 */
fun Db.stories(
    geo: List<Double>,
    nearbyMaxDistance: Double,
    offset: Int,
    limit: Int
) = list(
    Story::class,
    """
        for x in @@collection
            filter x.${f(Story::published)} == true
            let g = x.${f(Story::geo)} == null ? document(${Person::class.collection()}, x.${f(Story::person)}).${f(Person::geo)} : x.${f(Story::geo)}
            let d = g == null ? null : distance(g[0], g[1], @geo[0], @geo[1])
            filter d != null and d <= @nearbyMaxDistance
            sort x.${f(Story::publishDate)} desc, x.${f(Story::createdAt)} desc
            limit @offset, @limit
            return ${storyExtended(null, "x")}
    """.trimIndent(),
    mapOf(
        "geo" to geo,
        "nearbyMaxDistance" to nearbyMaxDistance,
        "offset" to offset,
        "limit" to limit
    )
)

/**
 * @geo The geolocation bias
 * @person The current user
 */
fun Db.countStories(geo: List<Double>, person: String, nearbyMaxDistance: Double, after: Instant?, public: Boolean = false) = query(
    Int::class,
    """
        return count(
            for x in ${Story::class.collection()}
                filter x.${f(Story::published)} == true
                    and (@after == null or x.${f(Story::publishDate)} > @after)
                let d = x.${f(Story::geo)} == null ? null : distance(x.${f(Story::geo)}[0], x.${f(Story::geo)}[1], @geo[0], @geo[1])
                filter (
                    (d != null and d <= @nearbyMaxDistance and @public) or x.${f(Story::person)} == @personKey or first(
                        for group in outbound @person graph `${Member::class.graph()}`
                            for friend, member in inbound group graph `${Member::class.graph()}`
                                filter member.${f(Member::gone)} != true
                                    and friend._key == x.${f(Story::person)}
                                limit 1
                                return true
                    ) == true
                )
                return true
        )
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "personKey" to person,
        "geo" to geo,
        "nearbyMaxDistance" to nearbyMaxDistance,
        "after" to after,
        "public" to public,
    )
).first()!!
