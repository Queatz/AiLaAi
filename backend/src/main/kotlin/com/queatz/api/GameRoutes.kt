package com.queatz.api

import com.queatz.api.urlize
import com.queatz.db.Comment
import com.queatz.db.CommentExtended
import com.queatz.db.GameDiscussion
import com.queatz.db.GameDiscussionExtended
import com.queatz.db.GameScene
import com.queatz.db.GameTile
import com.queatz.db.GameObject
import com.queatz.db.GameMusic
import com.queatz.db.Person
import com.queatz.db.asId
import com.queatz.db.collection
import com.queatz.db.commentsOf
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

fun Route.gameRoutes() {
    // GameScene routes
    authenticate(optional = true) {
        get("/game-scene/{id}") {
            respond {
                val gameScene = db.document(GameScene::class, parameter("id"))

                if (gameScene == null || (gameScene.published != true && gameScene.person != meOrNull?.id)) {
                    HttpStatusCode.NotFound
                } else {
                    gameScene
                }
            }
        }

        get("/game-scene/url/{url}") {
            respond {
                val url = parameter("url")

                // Query for a game scene with the given URL
                val query = """
                    FOR scene IN `${GameScene::class.collection()}`
                    FILTER (scene.url == @url OR scene._key == @url) AND (scene.published == true OR scene.person == @person)
                    LIMIT 1
                    RETURN scene
                """

                val scenes = db.query(
                    GameScene::class,
                    query,
                    mapOf("url" to url, "person" to meOrNull?.id)
                ).toList()

                if (scenes.isEmpty()) {
                    HttpStatusCode.NotFound
                } else {
                    scenes.first()
                }
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
                        gameScene.name = update.name
                    }
                    if (update.url != null) {
                        if (update.url.isNullOrBlank()) {
                            gameScene.url = null
                        } else {
                            val url = update.url!!.urlize()

                            // Check if URL is already in use by another scene
                            val query = """
                                FOR scene IN `${GameScene::class.collection()}`
                                FILTER scene.url == @url AND scene._key != @id
                                LIMIT 1
                                RETURN scene
                            """

                            val existingScene = db.query(
                                GameScene::class,
                                query,
                                mapOf("url" to url, "id" to gameScene.id)
                            ).firstOrNull()

                            if (existingScene != null) {
                                return@respond HttpStatusCode.Conflict.description("URL is already in use")
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

                    db.update(gameScene)
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

    // GameTile routes
    authenticate(optional = true) {
        get("/game-tile/{id}") {
            respond {
                val gameTile = db.document(GameTile::class, parameter("id"))

                if (gameTile == null || (gameTile.published != null && gameTile.person != meOrNull?.id)) {
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
                        gameTile.name = update.name
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

    // GameObject routes
    authenticate(optional = true) {
        get("/game-object/{id}") {
            respond {
                val gameObject = db.document(GameObject::class, parameter("id"))

                if (gameObject == null || (gameObject.published != null && gameObject.person != meOrNull?.id)) {
                    HttpStatusCode.NotFound
                } else {
                    gameObject
                }
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
                        published = gameObject.published
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
                    if (update.categories != null) {
                        gameObject.categories = update.categories
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

    // GameMusic routes
    authenticate(optional = true) {
        get("/game-music/{id}") {
            respond {
                val gameMusic = db.document(GameMusic::class, parameter("id"))

                if (gameMusic == null || (gameMusic.published != true && gameMusic.person != meOrNull?.id)) {
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
                        gameMusic.name = update.name
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

    // GameDiscussion routes
    authenticate(optional = true) {
        get("/game-scene/{id}/discussions") {
            respond {
                val gameScene = db.document(GameScene::class, parameter("id"))

                if (gameScene == null || (gameScene.published != true && gameScene.person != meOrNull?.id)) {
                    HttpStatusCode.NotFound
                } else {
                    // Query for discussions in this scene
                    val query = """
                        FOR d IN `${GameDiscussion::class.collection()}`
                        FILTER d.scene == @sceneId AND (d.resolved != true)
                        SORT d.createdAt DESC
                        LET person = DOCUMENT(`${Person::class.collection()}`, d.person)
                        RETURN {
                            discussion: d,
                            person: person,
                            comments: []
                        }
                    """

                    val discussions = db.query(
                        GameDiscussionExtended::class,
                        query,
                        mapOf("sceneId" to gameScene.id)
                    ).toList()

                    // Load comments for each discussion using the helper
                    discussions.mapNotNull { discussion ->
                        discussion.discussion?.id?.let { discussionId ->
                            val discussionIdAsId = discussionId.asId(GameDiscussion::class)
                            val comments = db.commentsOf(discussionIdAsId)
                            GameDiscussionExtended(
                                discussion = discussion.discussion,
                                person = discussion.person,
                                comments = comments
                            )
                        } ?: discussion
                    }
                }
            }
        }

        get("/game-discussion/{id}") {
            respond {
                val query = """
                        for d in `${GameDiscussion::class.collection()}`
                            FILTER d._key == @discussionId
                            LET person = DOCUMENT(`${Person::class.collection()}`, d.person)
                            RETURN {
                                discussion: d,
                                person: person,
                                comments: []
                            }
                        """

                val discussion = db.query(
                    klass = GameDiscussionExtended::class,
                    query = query,
                    parameters = mapOf("discussionId" to parameter("id"))
                ).firstOrNull() ?: return@respond HttpStatusCode.NotFound

                // Load comments for the discussion using the helper
                discussion.discussion?.id?.let { discussionId ->
                    val discussionIdAsId = discussionId.asId(GameDiscussion::class)
                    val comments = db.commentsOf(discussionIdAsId)
                    GameDiscussionExtended(
                        discussion = discussion.discussion,
                        person = discussion.person,
                        comments = comments
                    )
                } ?: discussion
            }
        }
    }

    authenticate {
        post("/game-scene/{id}/discussion") {
            respond {
                val gameScene = db.document(GameScene::class, parameter("id"))
                val person = me

                if (gameScene == null) {
                    HttpStatusCode.NotFound
                } else {
                    val newDiscussion = call.receive<GameDiscussion>()

                    if (newDiscussion.position == null) {
                        HttpStatusCode.BadRequest.description("Position is required")
                    } else {
                        val discussion = db.insert(
                            GameDiscussion(
                                person = person.id!!,
                                scene = gameScene.id!!,
                                title = newDiscussion.title,
                                position = newDiscussion.position,
                                comment = newDiscussion.comment
                            )
                        )

                        discussion
                    }
                }
            }
        }

        post("/game-discussion/{id}/comment") {
            respond {
                val discussion = db.document(GameDiscussion::class, parameter("id"))

                if (discussion == null) {
                    HttpStatusCode.NotFound
                } else {
                    val newComment = call.receive<Comment>()

                    if (newComment.comment.isNullOrBlank()) {
                        HttpStatusCode.BadRequest.description("Missing comment")
                    } else {
                        val comment = db.insert(
                            Comment().apply {
                                comment = newComment.comment
                                from = me.id!!.asId(Person::class)
                                to = discussion.id!!.asId(GameDiscussion::class)
                            }
                        )

                        comment
                    }
                }
            }
        }

        post("/game-discussion/{id}/delete") {
            respond {
                val discussion = db.document(GameDiscussion::class, parameter("id"))
                val person = me

                if (discussion == null) {
                    HttpStatusCode.NotFound
                } else if (discussion.person != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    db.delete(discussion)
                    HttpStatusCode.NoContent
                }
            }
        }

        post("/game-discussion/{id}/resolve") {
            respond {
                val discussion = db.document(GameDiscussion::class, parameter("id"))
                val person = me

                if (discussion == null) {
                    HttpStatusCode.NotFound
                } else if (discussion.person != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    discussion.resolved = true
                    db.update(discussion)
                    discussion
                }
            }
        }
    }
}
