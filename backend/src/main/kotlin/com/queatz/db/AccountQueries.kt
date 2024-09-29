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
