package com.queatz.api

import com.queatz.db.InventoryItem
import com.queatz.db.Trade
import com.queatz.db.TradeExtended
import com.queatz.db.TradeItem
import com.queatz.db.TradeMember
import com.queatz.db.activeTrades
import com.queatz.db.completedTrades
import com.queatz.db.inventoryItems
import com.queatz.db.inventoryOfPerson
import com.queatz.db.people
import com.queatz.db.trade
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.notify
import com.queatz.plugins.respond
import com.queatz.push.TradeEvent
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.time.Clock

fun Route.tradeRoutes() {
    authenticate {
        post("/trades") {
            respond {
                val me = me
                val newTrade = call.receive<Trade>()

                val people = newTrade.people?.takeIf { it.size >= 2 }
                    ?: return@respond HttpStatusCode.BadRequest.description("Trade must have 2 or more people")

                if (!people.contains(me.id!!)) {
                    return@respond HttpStatusCode.BadRequest.description("Must include yourself in the trade")
                }

                db.insert(
                    Trade(
                        people = people,
                        members = people.map {
                            TradeMember(
                                person = it,
                                confirmed = false,
                                items = emptyList(),
                            )
                        },
                        initiator = me.id!!
                    )
                ).also {
                    notify.trade(it, me, people = db.people(people), TradeEvent.Started)
                }
            }
        }

        get("/trades") {
            respond {
                db.activeTrades(me.id!!)
            }
        }

        get("/trades/complete") {
            respond {
                db.completedTrades(
                    me.id!!,
                    offset = call.parameters["offset"]?.toInt() ?: 0,
                    limit = call.parameters["limit"]?.toInt() ?: 20
                )
            }
        }

        get("/trades/{id}") {
            respond {
                db.trade(me.id!!, parameter("id")) ?: return@respond HttpStatusCode.NotFound
            }
        }

        post("/trades/{id}") {
            respond {
                val updatedTrade = call.receive<Trade>()
                val trade = getTrade(me.id!!, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                alreadyConfirmedResponse(trade)?.let {
                    return@respond it
                }

                completedOrCancelledResponse(trade)?.let {
                    return@respond it
                }

                updateTrade(trade) {
                    if (updatedTrade.note != null) {
                        note = updatedTrade.note
                    }

                    if (updatedTrade.photos != null) {
                        photos = updatedTrade.photos
                    }
                } ?: HttpStatusCode.BadRequest.description("Update failed")
            }
        }

        post("/trades/{id}/items") {
            respond {
                val me = me
                val items = call.receive<List<TradeItem>>()
                val trade = getTrade(me.id!!, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                alreadyConfirmedResponse(trade)?.let {
                    return@respond it
                }

                completedOrCancelledResponse(trade)?.let {
                    return@respond it
                }

                // todo optimize
                val myInventoryItems = db.inventoryItems(db.inventoryOfPerson(me.id!!).id!!)
                    .map { it.inventoryItem!! }.toSet()
                val myInventoryItemIds = myInventoryItems.map { it.id!! }.toSet()

                // Check items are in inventory
                if (
                    items.any { item ->
                        item.inventoryItem !in myInventoryItemIds
                    }
                ) {
                    return@respond HttpStatusCode.BadRequest.description("Item(s) not in inventory")
                }

                // Check quantity is in inventory
                if (
                    items.map { it.inventoryItem }.distinct().any { inventoryItem ->
                        items
                            .filter { it.inventoryItem == inventoryItem }
                            .sumOf { it.quantity!! } !in 0.0..myInventoryItems
                                .first { it.id == inventoryItem }
                                .quantity!!
                    }
                ) {
                    return@respond HttpStatusCode.BadRequest.description("Item(s) succeed quantity in inventory")
                }

                // Check person giving to is in trade
                if (
                    !items.all { item ->
                        item.to!! in trade.people!!
                    }
                ) {
                    return@respond HttpStatusCode.BadRequest.description("Person is not in this trade")
                }

                val myMember = trade.members!!.first {
                    it.person == me.id!!
                }

                updateTrade(trade) {
                    myMember.items = items
                } ?: HttpStatusCode.BadRequest.description("Update failed")
            }
        }

        post("/trades/{id}/confirm") {
            respond {
                val me = me
                val trade = getTrade(me.id!!, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                completedOrCancelledResponse(trade)?.let {
                    return@respond it
                }

                if (trade.members?.any { it.person == me.id!! && it.confirmed == true } == true) {
                    return@respond HttpStatusCode.BadRequest.description("Already confirmed")
                }

                val tradeState = call.receive<Trade>()

                if (!trade.isSame(tradeState)) {
                    return@respond HttpStatusCode.BadRequest.description("Trade has changed")
                }

                if (tradeState.note != trade.note) {
                    return@respond HttpStatusCode.BadRequest.description("Trade note has changed")
                }

                val myMember = trade.members!!.first {
                    it.person == me.id!!
                }

                updateTrade(trade) {
                    myMember.confirmed = true
                } ?: HttpStatusCode.BadRequest.description("Update failed")
            }
        }

        post("/trades/{id}/unconfirm") {
            respond {
                val me = me
                val trade = getTrade(me.id!!, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                completedOrCancelledResponse(trade)?.let {
                    return@respond it
                }

                if (trade.members?.any { it.person == me.id!! && it.confirmed == true } != true) {
                    return@respond HttpStatusCode.BadRequest.description("Already not confirmed")
                }

                val myMember = trade.members!!.first {
                    it.person == me.id!!
                }

                updateTrade(trade) {
                    myMember.confirmed = false
                } ?: HttpStatusCode.BadRequest.description("Update failed")
            }
        }

        post("/trades/{id}/cancel") {
            respond {
                val me = me
                val trade = getTrade(me.id!!, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                completedOrCancelledResponse(trade)?.let {
                    return@respond it
                }

                updateTrade(trade) {
                    cancelledAt = Clock.System.now()
                } ?: HttpStatusCode.BadRequest.description("Update failed")
            }
        }
    }
}

private fun Trade.isSame(trade: Trade): Boolean = note == trade.note &&
        members!!.zip(trade.members!!).all {
            it.first.items!!.toSet() == it.second.items!!.toSet()
        }

private fun RoutingContext.updateTrade(
    trade: Trade,
    block: Trade.() -> Unit
): TradeExtended? {
    trade.apply(block)

    if (trade.cancelledAt != null) {
        notify.trade(trade, me, people = db.people(trade.people!!), event = TradeEvent.Cancelled)
    } else if (trade.completedAt == null && trade.members!!.all { it.confirmed == true }) {
        if (!performTrade(trade)) return null
        trade.completedAt = Clock.System.now()
        notify.trade(trade, me, people = db.people(trade.people!!), event = TradeEvent.Completed)
    } else {
        notify.trade(trade, me, people = db.people(trade.people!!), event = TradeEvent.Updated)
    }

    db.update(trade)

    return db.trade(me.id!!, trade.id!!)!!
}

// todo, ensure all quantities are still good (i.e. no other trades have happened)
private fun performTrade(trade: Trade): Boolean {
    val inventoryItems = trade.members!!.flatMap { it.items!! }.map { it.inventoryItem!! }.distinct().map {
        db.document(InventoryItem::class, it)!!
    }
    val toInventories = trade.members!!.map { it.person!! }.map {
        db.inventoryOfPerson(it)
    }
    val allTradeItems = trade.members!!.flatMap { it.items!! }

    // Quantity check
    if (
        inventoryItems.any { inventoryItem ->
            allTradeItems
                .filter { it.inventoryItem!! == inventoryItem.id!! }
                .sumOf { it.quantity!! } > inventoryItem.quantity!!
        }
    ) {
        return false
    }

    trade.members!!.forEach { member ->
        member.items!!.forEach { tradeItem ->
            val inventoryItem = inventoryItems.first { it.id!! == tradeItem.inventoryItem!! }
            val toPersonInventory = toInventories.first { it.person!! == tradeItem.to!! }

            if (tradeItem.quantity == inventoryItem.quantity) {
                // Give all (move items)
                // todo: add movement to history
                db.update(
                    inventoryItem.apply {
                        inventory = toPersonInventory.id!!
                        equipped = false
                    }
                )
            } else {
                // Give some (split items)
                // todo: add split to history
                db.update(
                    inventoryItem.apply {
                        quantity = quantity!! - tradeItem.quantity!!
                    }
                )
                db.insert(
                    InventoryItem(
                        inventory = toPersonInventory.id!!,
                        item = inventoryItem.item!!,
                        quantity = tradeItem.quantity!!,
                        expiresAt = inventoryItem.expiresAt
                    )
                )
            }
        }
    }

    return true
}

private fun alreadyConfirmedResponse(trade: Trade): HttpStatusCode? =
    if (trade.members!!.any { it.confirmed == true }) {
        HttpStatusCode.BadRequest.description("Trade already has confirmations")
    } else {
        null
    }

private fun completedOrCancelledResponse(trade: Trade): HttpStatusCode? = when {
    trade.completedAt != null -> {
        HttpStatusCode.BadRequest.description("Trade already completed")
    }

    trade.cancelledAt != null -> {
        HttpStatusCode.BadRequest.description("Trade already cancelled")
    }

    else -> null
}

private fun getTrade(person: String, trade: String) = db.document(Trade::class, trade)?.takeIf {
    person in (it.people ?: emptyList())
}
