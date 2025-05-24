package com.queatz.api

import com.queatz.db.GameTile
import com.queatz.db.collection
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.gameTileRoutes() {
    // GameTile routes
    authenticate(optional = true) {
        get("/game-tile/{id}") {
            respond {
                val gameTile = db.document(GameTile::class, parameter("id"))

                if (gameTile == null) {
                    HttpStatusCode.NotFound
                } else {
                    gameTile
                }
            }
        }

        get("/game-tile") {
            respond {
                val person = meOrNull

                // If user is logged in, get their tiles + all published tiles
                // If not logged in, only get published tiles
                val query = if (person != null) {
                    """
                    FOR tile IN `${GameTile::class.collection()}`
                    FILTER tile.published != null OR tile.person == @person
                    SORT tile.createdAt DESC
                    RETURN tile
                    """
                } else {
                    """
                    FOR tile IN `${GameTile::class.collection()}`
                    FILTER tile.published != null
                    SORT tile.createdAt DESC
                    RETURN tile
                    """
                }

                db.query(
                    GameTile::class,
                    query,
                    mapOf("person" to person?.id)
                ).toList()
            }
        }
    }

    authenticate {
        post("/game-tile") {
            respond {
                val person = me
                val gameTile = call.receive<GameTile>()

                db.insert(
                    GameTile(
                        person = person.id!!,
                        photo = gameTile.photo,
                        offset = gameTile.offset,
                        published = gameTile.published
                    )
                )
            }
        }

        post("/game-tile/{id}") {
            respond {
                val gameTile = db.document(GameTile::class, parameter("id"))
                val person = me

                if (gameTile == null) {
                    HttpStatusCode.NotFound
                } else if (gameTile.person != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    val update = call.receive<GameTile>()

                    // Once published, can never be unpublished
                    if (gameTile.published != null && update.published == null) {
                        return@respond HttpStatusCode.BadRequest.description("Cannot unpublish a published game tile")
                    }

                    if (update.photo != null) {
                        gameTile.photo = update.photo
                    }
                    if (update.offset != null) {
                        gameTile.offset = update.offset
                    }
                    if (update.published != null) {
                        gameTile.published = update.published
                    }
                    if (update.name != null) {
                        gameTile.name = update.name?.trim()
                    }
                    if (update.description != null) {
                        gameTile.description = update.description?.trim()
                    }
                    if (update.categories != null) {
                        gameTile.categories = update.categories
                    }

                    db.update(gameTile)
                }
            }
        }

        post("/game-tile/{id}/delete") {
            respond {
                val gameTile = db.document(GameTile::class, parameter("id"))
                val person = me

                if (gameTile == null) {
                    HttpStatusCode.NotFound
                } else if (gameTile.person != person.id) {
                    HttpStatusCode.Forbidden
                } else if (gameTile.published != null) {
                    HttpStatusCode.BadRequest.description("Cannot delete a published game tile")
                } else {
                    db.delete(gameTile)
                    HttpStatusCode.NoContent
                }
            }
        }
    }
}
