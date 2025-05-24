package com.queatz.api

import com.queatz.db.GameObject
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

fun Route.gameObjectRoutes() {
    // GameObject routes
    authenticate(optional = true) {
        get("/game-object/{id}") {
            respond {
                db.document(GameObject::class, parameter("id")) ?: HttpStatusCode.NotFound
            }
        }

        get("/game-object") {
            respond {
                val person = meOrNull

                // If user is logged in, get their objects + all published objects
                // If not logged in, only get published objects
                val query = if (person != null) {
                    """
                    FOR obj IN `${GameObject::class.collection()}`
                    FILTER obj.published != null OR obj.person == @person
                    SORT obj.createdAt DESC
                    RETURN obj
                    """
                } else {
                    """
                    FOR obj IN `${GameObject::class.collection()}`
                    FILTER obj.published != null
                    SORT obj.createdAt DESC
                    RETURN obj
                    """
                }

                db.query(
                    GameObject::class,
                    query,
                    mapOf("person" to person?.id)
                ).toList()
            }
        }
    }

    authenticate {
        post("/game-object") {
            respond {
                val person = me
                val gameObject = call.receive<GameObject>()

                db.insert(
                    GameObject(
                        person = person.id!!,
                        photo = gameObject.photo,
                        width = gameObject.width,
                        height = gameObject.height,
                        published = gameObject.published,
                        options = gameObject.options,
                    )
                )
            }
        }

        post("/game-object/{id}") {
            respond {
                val gameObject = db.document(GameObject::class, parameter("id"))
                val person = me

                if (gameObject == null) {
                    HttpStatusCode.NotFound
                } else if (gameObject.person != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    val update = call.receive<GameObject>()

                    // Once published, can never be unpublished
                    if (gameObject.published != null && update.published == null) {
                        return@respond HttpStatusCode.BadRequest.description("Cannot unpublish a published game object")
                    }

                    if (update.photo != null) {
                        gameObject.photo = update.photo
                    }
                    if (update.width != null) {
                        gameObject.width = update.width
                    }
                    if (update.height != null) {
                        gameObject.height = update.height
                    }
                    if (update.published != null) {
                        gameObject.published = update.published
                    }
                    if (update.name != null) {
                        gameObject.name = update.name?.trim()
                    }
                    if (update.description != null) {
                        gameObject.description = update.description?.trim()
                    }
                    if (update.categories != null) {
                        gameObject.categories = update.categories
                    }

                    if (update.options != null) {
                        gameObject.options = update.options
                    }

                    db.update(gameObject)
                }
            }
        }

        post("/game-object/{id}/delete") {
            respond {
                val gameObject = db.document(GameObject::class, parameter("id"))
                val person = me

                if (gameObject == null) {
                    HttpStatusCode.NotFound
                } else if (gameObject.person != person.id) {
                    HttpStatusCode.Forbidden
                } else if (gameObject.published != null) {
                    HttpStatusCode.BadRequest.description("Cannot delete a published game object")
                } else {
                    db.delete(gameObject)
                    HttpStatusCode.NoContent
                }
            }
        }
    }
}
