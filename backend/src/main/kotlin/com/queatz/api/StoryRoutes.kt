package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.*
import com.queatz.push.StoryEvent
import com.queatz.receiveFile
import com.queatz.receiveFiles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString

fun Route.storyRoutes() {
    authenticate(optional = true) {
        get("/stories/{id}") {
            respond {
                db.story(meOrNull?.id?.asId(Person::class), parameter("id"))
                //?.takeIf { it.published == true || it.person == meOrNull?.id } // todo, authorize? how to share a draft
                    ?: HttpStatusCode.NotFound
            }
        }

        get("/urls/stories/{url}") {
            respond {
                db.storyByUrl(meOrNull?.id?.asId(Person::class), parameter("url"))
                //?.takeIf { it.published == true || it.person == meOrNull?.id } // todo, authorize? how to share a draft
                    ?: HttpStatusCode.NotFound
            }
        }

        get("/stories/{id}/reactions") {
            respond {
                db.reactionsOf(parameter("id").asId(Story::class))
            }
        }

        get("/stories/{id}/comments") {
            respond {
                db.commentsOf(parameter("id").asId(Story::class))
            }
        }

        get("/stories") {
            respond {
                val geo = call.parameters["geo"]?.split(",")?.map { it.toDouble() } ?: emptyList()

                if (geo.size != 2) {
                    return@respond HttpStatusCode.BadRequest.description("'geo' must be an array of size 2")
                }

                meOrNull.let { me ->
                    if (me == null) {
                        db.stories(
                            geo,
                            defaultNearbyMaxDistanceInMeters,
                            call.parameters["offset"]?.toInt() ?: 0,
                            call.parameters["limit"]?.toInt() ?: 20
                        )
                    } else {
                        call.parameters["public"]?.toBoolean().let {
                            if (it != true) {
                                db.stories(
                                    geo = geo,
                                    person = me.id!!,
                                    nearbyMaxDistance = defaultNearbyMaxDistanceInMeters,
                                    offset = call.parameters["offset"]?.toInt() ?: 0,
                                    limit = call.parameters["limit"]?.toInt() ?: 20
                                )
                            } else {
                                db.stories(
                                    geo = geo,
                                    person = me.id!!,
                                    nearbyMaxDistance = defaultNearbyMaxDistanceInMeters,
                                    offset = call.parameters["offset"]?.toInt() ?: 0,
                                    limit = call.parameters["limit"]?.toInt() ?: 20,
                                    public = it
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    authenticate {
        post("/stories") {
            respond {
                val story = call.receive<Story>()
                db.insert(
                    Story(
                        title = story.title,
                        content = story.content,
                        geo = story.geo,
                        categories = story.categories,
                        person = me.id!!
                    )
                )
            }
        }

        post("/stories/{id}") {
            respond {
                val story = db.document(Story::class, parameter("id"))
                    ?.takeIf { it.person == me.id } // authorize
                    ?: return@respond HttpStatusCode.NotFound

                if (story.published == true) {
                    return@respond HttpStatusCode.BadRequest.description("Story is published")
                }

                val update = call.receive<Story>()

                val numbers = "\\d".toRegex()

                fun String.uniqueInDb(id: String) = if (
                    all { numbers.matches("$it") }
                ) {
                    "story-$this"
                } else {
                    this
                }.let { url ->
                    "$url-$id"
                }

                // Publishing can't be combined with any other update
                if (story.published != true && update.published == true && story.publishDate == null) {
                    story.published = true
                    story.publishDate = Clock.System.now()
                    story.url = story.title!!.urlize().uniqueInDb(story.id!!)

                    // Share to groups
                    db.storyDraft(me.id!!, story.id!!)?.groups?.takeIf { it.isNotEmpty() }?.let { groupIds ->
                        val groups = db.groups(me.id!!, groupIds)
                        val attachment = json.encodeToString(StoryAttachment(story.id!!))
                        groups.forEach { group ->
                            val myMember = db.member(me.id!!, group.id!!) ?: return@forEach
                            db.insert(Message(group.id, myMember.id, text = null, attachment = attachment))

                            notify.message(
                                group = group,
                                person = me,
                                message = Message(text = null, attachment = attachment)
                            )

                            group.seen = Clock.System.now()
                            db.update(group)
                        }
                    }

                    // Share to subscribers
                    val authors = listOf(
                        db.document(Person::class, story.person!!)!!
                    )

                    notify.story(
                        story = story,
                        authors = authors,
                        subscribers = db.subscribersOf(authors.map { it.id!! }),
                        event = StoryEvent.Posted
                    )
                } else if (story.published != true) {
                    if (update.title != null) {
                        story.title = update.title
                    }

                    if (update.content != null) {
                        story.content = update.content
                    }

                    if (update.background != null) {
                        story.background = update.background
                    }

                    if (update.geo != null) {
                        story.geo = update.geo?.takeIf { it.isNotEmpty() }
                    }
                }

                db.update(story)
            }
        }

        post("/stories/{id}/photos") {
            respond {
                val storyId = parameter("id")
                var result: List<String>? = null
                call.receiveFiles("photo", "story-${storyId}") { photosUrls, _ ->
                    result = photosUrls
                }
                result ?: HttpStatusCode.InternalServerError
            }
        }

        post("/stories/{id}/audio") {
            respond {
                val storyId = parameter("id")
                var result: String? = null
                call.receiveFile("audio", "story-${storyId}") { audioUrl, _ ->
                    result = audioUrl
                }
                result ?: HttpStatusCode.InternalServerError
            }
        }

        post("/stories/{id}/delete") {
            respond {
                val story = db.document(Story::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound
                if (story.person == me.id) {
                    db.delete(story)
                    HttpStatusCode.NoContent
                } else {
                    HttpStatusCode.Forbidden
                }
            }
        }

        post("/stories/{id}/react") {
            respond {
                val story = db.document(Story::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

                if (story.published != true) {
                    return@respond HttpStatusCode.BadRequest.description("Story is not yet published")
                }

                val react = call.receive<ReactBody>()

                if (react.remove == true) {
                    db.unreact(
                        from = me.id!!.asId(Person::class),
                        to = story.id!!.asId(Story::class),
                        reaction = react.reaction.reaction!!.take(64)
                    )
                } else {
                    db.react(
                        from = me.id!!.asId(Person::class),
                        to = story.id!!.asId(Story::class),
                        reaction = react.reaction.reaction!!.take(64),
                        comment = react.reaction.comment
                    )
                }

                HttpStatusCode.NoContent
            }
        }

        post("/stories/{id}/comment") {
            respond {
                val me = me
                val story = db.document(Story::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

                if (story.published != true) {
                    return@respond HttpStatusCode.BadRequest.description("Story is not yet published")
                }

                val newComment = call.receive<Comment>()

                if (newComment.comment.isNullOrBlank()) {
                    return@respond HttpStatusCode.BadRequest.description("Missing comment")
                }

                val comment = db.insert(
                    Comment().apply {
                        comment = newComment.comment
                        from = me.id!!.asId(Person::class)
                        to = story.id!!.asId(Story::class)
                    }
                )

                notify.comment(
                    story,
                    comment,
                    me
                )

                comment
            }
        }

        get("/stories/{id}/draft") {
            respond {
                db.storyDraft(me.id!!, parameter("id"))?.also {
                    it.groups?.let { groups ->
                        it.groupDetails = db.groups(me.id!!, groups)
                    }
                } ?: HttpStatusCode.NotFound
            }
        }

        post("/stories/{id}/draft") {
            respond {
                val draft = db.storyDraft(me.id!!, parameter("id")) ?: StoryDraft(
                    story = parameter("id")
                ).let {
                    db.insert(it)
                }

                val update = call.receive<StoryDraft>()

                if (update.groups != null) {
                    draft.groups = update.groups
                }

                db.update(draft)
            }
        }
    }
}

fun String.urlize() = "\\W+".toRegex().replace(trim(), "-").trim('-').encodeURLPathPart()
