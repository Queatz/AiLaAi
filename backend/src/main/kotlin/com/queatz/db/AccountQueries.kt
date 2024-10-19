package com.queatz.db

fun Db.account(person: String) = one(
    Account::class,
    """
        for account in @@collection
            filter account.${f(Account::person)} == @person
            return account
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

fun Db.accountsByPoints() = list(
    Account::class,
    """
        for account in @@collection
            filter account.${f(Account::points)} > 0
            sort account.${f(Account::points)} desc
            limit 100
            return account
    """.trimIndent()
)
