package com.queatz

import com.queatz.db.Account
import com.queatz.db.Card
import com.queatz.db.Person
import com.queatz.db.account
import com.queatz.plugins.db

val accounts = Accounts()

class Accounts {
    fun upgradeCardCost(card: Card): Int {
        return (card.level ?: 0).let { level ->
            pow(level.coerceAtLeast(0) + 1)
        }
    }

    fun canUpgradeCard(account: Account, card: Card): Boolean {
        return upgradeCardCost(card) <= (account.points ?: 0)
    }

    fun upgradeCard(account: Account, card: Card): Boolean {
        val cost = upgradeCardCost(card)

        if ((account.points ?: 0) < cost) {
            return false
        }

        account.points = account.points!! - cost
        card.level = (card.level ?: 0) + 1

        db.update(account)
        db.update(card)

        return true
    }

    fun account(personId: String): Account {
        return db.account(personId) ?: Account(person = personId).let {
            db.insert(it)
        }
    }

    fun addPoints(account: Account, points: Int) {
        account.points = (account.points ?: 0) + points
        db.update(account)

        // todo: send push
    }
}

private fun pow(x: Int) = 1 shl (x - 1)
