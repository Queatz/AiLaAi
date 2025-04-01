package com.queatz.db

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
 * @code The group to fetch invites for
 */
fun Db.activeInvitesOfGroup(group: String) = list(
    Invite::class,
    """
        for x in @@collection
            filter x.${f(Invite::group)} == @group
                and (x.${f(Invite::expiry)} == null or DATE_ISO8601(x.${f(Invite::expiry)}) > DATE_ISO8601(DATE_NOW()))
                and (x.${f(Invite::remaining)} == null or x.${f(Invite::remaining)} > 0)
            return x
    """.trimIndent(),
    mapOf(
        "group" to group
    )
)
