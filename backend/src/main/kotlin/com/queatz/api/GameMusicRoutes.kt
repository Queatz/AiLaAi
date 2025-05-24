package com.queatz.api

import com.queatz.db.GameMusic
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

fun Route.gameMusicRoutes() {
    // GameMusic routes
    authenticate(optional = true) {
        get("/game-music/{id}") {
            respond {
                val gameMusic = db.document(GameMusic::class, parameter("id"))

                if (gameMusic == null) {
                    HttpStatusCode.NotFound
                } else {
                    gameMusic
                }
            }
        }

        get("/game-music") {
            respond {
                val person = meOrNull

                // If user is logged in, get their music + all published music
                // If not logged in, only get published music
                val query = if (person != null) {
                    """
                    FOR music IN `${GameMusic::class.collection()}`
                    FILTER music.published == true OR music.person == @person
                    SORT music.createdAt DESC
                    RETURN music
                    """
                } else {
                    """
                    FOR music IN `${GameMusic::class.collection()}`
                    FILTER music.published == true
                    SORT music.createdAt DESC
                    RETURN music
                    """
                }

                db.query(
                    GameMusic::class,
                    query,
                    mapOf("person" to person?.id)
                ).toList()
            }
        }
    }

    authenticate {
        post("/game-music") {
            respond {
                val person = me
                val gameMusic = call.receive<GameMusic>()

                db.insert(
                    GameMusic(
                        person = person.id!!,
                        name = gameMusic.name,
                        audio = gameMusic.audio,
                        duration = gameMusic.duration,
                        published = gameMusic.published,
                        categories = gameMusic.categories
                    )
                )
            }
        }

        post("/game-music/{id}") {
            respond {
                val gameMusic = db.document(GameMusic::class, parameter("id"))
                val person = me

                if (gameMusic == null) {
                    HttpStatusCode.NotFound
                } else if (gameMusic.person != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    val update = call.receive<GameMusic>()

                    // Once published, can never be unpublished
                    if (gameMusic.published == true && update.published == false) {
                        return@respond HttpStatusCode.BadRequest.description("Cannot unpublish a published game music")
                    }

                    if (update.name != null) {
                        gameMusic.name = update.name?.trim()
                    }
                    if (update.description != null) {
                        gameMusic.description = update.description?.trim()
                    }
                    if (update.audio != null) {
                        gameMusic.audio = update.audio
                    }
                    if (update.duration != null) {
                        gameMusic.duration = update.duration
                    }
                    if (update.published != null) {
                        gameMusic.published = update.published
                    }
                    if (update.categories != null) {
                        gameMusic.categories = update.categories
                    }

                    db.update(gameMusic)
                }
            }
        }

        post("/game-music/{id}/delete") {
            respond {
                val gameMusic = db.document(GameMusic::class, parameter("id"))
                val person = me

                if (gameMusic == null) {
                    HttpStatusCode.NotFound
                } else if (gameMusic.person != person.id) {
                    HttpStatusCode.Forbidden
                } else if (gameMusic.published == true) {
                    HttpStatusCode.BadRequest.description("Cannot delete a published game music")
                } else {
                    db.delete(gameMusic)
                    HttpStatusCode.NoContent
                }
            }
        }
    }
}
