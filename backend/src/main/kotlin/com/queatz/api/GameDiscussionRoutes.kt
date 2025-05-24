package com.queatz.api

import com.queatz.db.Comment
import com.queatz.db.GameDiscussion
import com.queatz.db.GameDiscussionExtended
import com.queatz.db.GameScene
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

fun Route.gameDiscussionRoutes() {
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
