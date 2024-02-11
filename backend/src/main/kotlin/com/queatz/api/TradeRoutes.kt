package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.notify
import com.queatz.plugins.respond
import com.queatz.push.TradeEvent
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.datetime.Clock

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
                    notify.trade(it, people = db.people(people), TradeEvent.Started)
                }
            }
        }

        get("/trades") {
            respond {
                db.activeTrades(me.id!!)
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

                completedOrCancelledResponse(trade)?.let {
                    return@respond it
                }

                if (updatedTrade.note != null) {
                    updateTrade(trade) {
                        note = updatedTrade.note
                    }
                } else {
                    HttpStatusCode.BadRequest.description("Nothing to update")
                }
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
                    items.any { item ->
                        val inventoryItem = myInventoryItems.first { it.id == item.inventoryItem!! }
                        item.quantity!! <= 0.0 || item.quantity!! > inventoryItem.quantity!!
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
                }
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

                val myMember = trade.members!!.first {
                    it.person == me.id!!
                }

                updateTrade(trade) {
                    myMember.confirmed = true
                }
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
                }
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
                }
            }
        }
    }
}

private fun Trade.isSame(trade: Trade): Boolean = note == trade.note &&
        members!!.zip(trade.members!!).all {
            it.first.items!!.toSet() == it.second.items!!.toSet()
        }

private fun PipelineContext<*, ApplicationCall>.updateTrade(
    trade: Trade,
    block: Trade.() -> Unit
): TradeExtended {
    trade.apply(block)

    if (trade.cancelledAt != null) {
        notify.trade(trade, people = db.people(trade.people!!), event = TradeEvent.Cancelled)
    } else if (trade.completedAt == null && trade.members!!.all { it.confirmed == true }) {
        performTrade(trade)
        trade.completedAt = Clock.System.now()
        notify.trade(trade, people = db.people(trade.people!!), event = TradeEvent.Completed)
    } else {
        notify.trade(trade, people = db.people(trade.people!!), event = TradeEvent.Updated)
    }

    db.update(trade)

    return db.trade(me.id!!, trade.id!!)!!
}

// todo, ensure all quantities are still good (i.e. no other trades have happened)
private fun performTrade(trade: Trade) {
    trade.members!!.forEach { member ->
        member.items!!.forEach { tradeItem ->
            val inventoryItem = db.document(InventoryItem::class, tradeItem.inventoryItem!!)!!
            val toPersonInventory = db.inventoryOfPerson(tradeItem.to!!)

            if (tradeItem.quantity == inventoryItem.quantity) {
                // Give all (move items)
                // todo: add movement to history
                inventoryItem.inventory = toPersonInventory.id!!
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
