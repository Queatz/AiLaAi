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
