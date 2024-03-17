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
                        return@respond HttpStatusCode.BadRequest.description("Item is not in inventory")
                    }

                    if (drop.quantity <= 0.0 || drop.quantity > inventoryItem.quantity!!) {
                        return@respond HttpStatusCode.BadRequest.description("Quantity is not in inventory")
                    }

                    val dropInventory = drop.geo?.let { geo ->
                        db.inventoryOfGeo(geo) ?: db.insert(Inventory(geo = geo))
                    }

                    if (drop.quantity == inventoryItem.quantity) {
                        // Drop all
                        // todo: add to history
                        db.update(
                            inventoryItem.also {
                                it.inventory = dropInventory?.id
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
                                inventory = dropInventory?.id,
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

        get("/inventory/explore") {
            respond {
                val geo = parameter("geo").split(",").map { it.toDouble() }

                if (geo.size != 2) {
                    return@respond HttpStatusCode.BadRequest.description("'geo' must be an array of size 2")
                }

                db.inventoriesNearGeo(geo)
            }
        }

        get("/inventory/{id}") {
            respond {
                db.inventoryItems(parameter("id"))
            }
        }

        post("/inventory/{id}/take") {
            respond {
                val inventory = db.document(Inventory::class, parameter("id"))
                val take = call.receive<TakeInventoryBody>()
                // Take items from inventory with geo

            }
        }
    }
}
