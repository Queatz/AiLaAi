package com.queatz.db

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

val Db.totalItems
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Item::class.collection()}`
                    return true
            )
        """
    ).first()!!
