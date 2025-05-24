package com.queatz.api

import com.queatz.db.GameScene
import com.queatz.db.collection
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.gameSceneRoutes() {
    // GameScene routes
    authenticate(optional = true) {
        get("/game-scene/{url}") {
            respond {
                val url = parameter("url")

                // Query for a game scene with the given URL
                val query = """
                    FOR scene IN `${GameScene::class.collection()}`
                    FILTER (scene.url == @url OR scene._key == @url) AND (scene.published == true OR scene.person == @person)
                    LIMIT 1
                    RETURN scene
                """

                db.query(
                    GameScene::class,
                    query,
                    mapOf("url" to url, "person" to meOrNull?.id)
                ).firstOrNull() ?: HttpStatusCode.NotFound
            }
        }

        get("/game-scene") {
            respond {
                val person = meOrNull

                // If user is logged in, get their scenes + all published scenes
                // If not logged in, only get published scenes
                val query = if (person != null) {
                    """
                    FOR scene IN `${GameScene::class.collection()}`
                    FILTER scene.published == true OR scene.person == @person
                    SORT scene.createdAt DESC
                    RETURN scene
                    """
                } else {
                    """
                    FOR scene IN `${GameScene::class.collection()}`
                    FILTER scene.published == true
                    SORT scene.createdAt DESC
                    RETURN scene
                    """
                }

                db.query(
                    GameScene::class,
                    query,
                    mapOf("person" to person?.id)
                ).toList()
            }
        }
    }

    authenticate {
        post("/game-scene") {
            respond {
                val person = me
                val gameScene = call.receive<GameScene>()

                db.insert(
                    GameScene(
                        person = person.id!!,
                        name = gameScene.name,
                        tiles = gameScene.tiles,
                        objects = gameScene.objects,
                        config = gameScene.config,
                        published = gameScene.published
                    )
                )
            }
        }

        post("/game-scene/{id}") {
            respond {
                val gameScene = db.document(GameScene::class, parameter("id"))
                val person = me

                if (gameScene == null) {
                    HttpStatusCode.NotFound
                } else if (gameScene.person != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    val update = call.receive<GameScene>()

                    if (update.name != null) {
                        gameScene.name = update.name?.trim()
                    }
                    if (update.description != null) {
                        gameScene.description = update.description?.trim()
                    }
                    if (update.url != null) {
                        if (update.url.isNullOrBlank()) {
                            gameScene.url = null
                        } else {
                            val url = update.url!!.urlize()

                            // Check if URL is already in use by another scene
                            val query = """
                                FOR scene IN `${GameScene::class.collection()}`
                                FILTER LOWER(scene.url) == @url AND scene._key != @id
                                LIMIT 1
                                RETURN scene
                            """

                            val existingScene = db.query(
                                GameScene::class,
                                query,
                                mapOf("url" to url.lowercase(), "id" to gameScene.id)
                            ).firstOrNull()

                            if (existingScene != null) {
                                return@respond HttpStatusCode.Conflict
                            }

                            gameScene.url = url
                        }
                    }
                    if (update.tiles != null) {
                        gameScene.tiles = update.tiles
                    }
                    if (update.objects != null) {
                        gameScene.objects = update.objects
                    }
                    if (update.config != null) {
                        gameScene.config = update.config
                    }
                    if (update.published != null) {
                        gameScene.published = update.published
                    }
                    if (update.categories != null) {
                        gameScene.categories = update.categories
                    }
                    if (update.photo != null) {
                        gameScene.photo = update.photo
                    }

                    db.update(gameScene)
                    gameScene
                }
            }
        }

        post("/game-scene/{id}/delete") {
            respond {
                val gameScene = db.document(GameScene::class, parameter("id"))
                val person = me

                if (gameScene == null) {
                    HttpStatusCode.NotFound
                } else if (gameScene.person != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    db.delete(gameScene)
                    HttpStatusCode.NoContent
                }
            }
        }
    }
}
