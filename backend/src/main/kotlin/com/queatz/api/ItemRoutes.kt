package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds


fun Route.itemRoutes() {
    authenticate {
        get("/me/inventory") {
            respond {
                db.inventoryItems(db.inventoryOfPerson(me.id!!).id!!)
            }
        }

        post("/inventory/{id}/drop") {
            respond {
                call.receive<DropItemBody>().let { drop ->
                    val inventoryItem = db.document(InventoryItem::class, parameter("id")) ?:
                        return@respond HttpStatusCode.NotFound

                    val inventory = db.inventoryOfPerson(me.id!!)

                    if (inventoryItem.inventory != inventory.id) {
                        return@respond HttpStatusCode.BadRequest.description("Item not in inventory")
                    }

                    if (drop.quantity <= 0.0 || drop.quantity > inventoryItem.quantity!!) {
                        return@respond HttpStatusCode.BadRequest.description("Quantity not in inventory")
                    }

                    if (drop.quantity == inventoryItem.quantity) {
                        // Drop all
                        // todo: add to history
                        db.update(
                            inventoryItem.also {
                                it.inventory = null
                                it.expiresAt = Clock.System.now()
                            }
                        )
                    } else {
                        // Drop some
                        // todo: add to history
                        db.update(
                            inventoryItem.also {
                                it.quantity = it.quantity!! - drop.quantity
                            }
                        )
                        db.insert(
                            InventoryItem(
                                inventory = null,
                                item = inventoryItem.item!!,
                                quantity = drop.quantity,
                                expiresAt = Clock.System.now()
                            )
                        )
                    }
                }

                HttpStatusCode.OK
            }
        }

        get("/items") {
            respond {
                db.myItems(me.id!!, db.inventoryOfPerson(me.id!!).id!!)
            }
        }

        post("/items") {
            respond {
                call.receive<Item>().let {
                    db.insert(
                        Item(
                            creator = me.id!!,
                            name = it.name,
                            photo = it.photo,
                            description = it.description,
                            divisible = it.divisible,
                            lifespan = it.lifespan,
                            becomes = it.becomes
                        )
                    )
                }
            }
        }

        post("/items/{id}/mint") {
            respond {
                call.receive<MintItemBody>().let {
                    if (it.quantity <= 0.0) {
                        return@respond HttpStatusCode.BadRequest.description("Too few quantity")
                    }

                    val item = db.document(Item::class, parameter("id"))
                        ?: return@respond HttpStatusCode.NotFound

                    if (item.creator != me.id) {
                        return@respond HttpStatusCode.BadRequest
                    }

                    val inventory = db.inventoryOfPerson(me.id!!)

                    db.insert(
                        InventoryItem(
                            inventory = inventory.id!!,
                            item = item.id!!,
                            quantity = it.quantity,
                            expiresAt = item.lifespan?.seconds?.let { Clock.System.now() + it }
                        )
                    )
                }
            }
        }
    }
}
