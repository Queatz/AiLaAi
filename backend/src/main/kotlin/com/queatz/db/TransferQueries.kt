package com.queatz.db

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
