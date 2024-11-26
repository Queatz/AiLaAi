package com.queatz.api

import com.queatz.db.Card
import com.queatz.db.CardAttachment
import com.queatz.db.CreateWidgetBody
import com.queatz.db.Message
import com.queatz.db.RunWidgetBody
import com.queatz.db.RunWidgetResponse
import com.queatz.db.StoryContent
import com.queatz.db.Widget
import com.queatz.db.member
import com.queatz.db.toJsonStoryContent
import com.queatz.db.widget
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.json
import com.queatz.plugins.me
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import com.queatz.widgets.FormValue
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormFieldType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean


fun Route.widgetRoutes() {
    authenticate(optional = true) {
        get("/widgets/{id}") {
            respond {
                db.document(Widget::class, parameter("id")) ?: HttpStatusCode.NotFound
            }
        }

        post("/widgets/{id}/delete") {
            respond {
                val widget = db.widget(me.id!!, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                db.delete(widget)

                HttpStatusCode.OK
            }
        }

        post("/widgets/{id}/run") {
            respond {
                val widget = db.document(Widget::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                val body = call.receive<RunWidgetBody>()

                when (widget.widget) {
                    Widgets.Form -> {
                        val submitter = meOrNull
                        val formOwner = widget.person!!
                        val data = json.decodeFromString<FormData>(widget.data!!)

                        // Verify the form owner is the owner of the destination page
                        if (data.page == null) {
                            return@respond HttpStatusCode.BadRequest.description("Form is missing a page to submit to")
                        }

                        val formValues = json.decodeFromString<List<FormValue>>(body.data!!)

                        val submitterContent = if (submitter != null) {
                            listOf(
                                // todo: translate
                                StoryContent.Section("Submitted by"),
                                StoryContent.Profiles(listOf(submitter.id!!))
                            )
                        } else {
                            listOf(
                                // todo: translate
                                StoryContent.Section("Submitted by"),
                                // todo: translate
                                StoryContent.Text("Anonymous")
                            )
                        }

                        val content = (submitterContent + formValues.map { formValue ->
                            listOf(
                                StoryContent.Section(formValue.title),
                                when (formValue.type) {
                                     FormFieldType.Input -> {
                                        StoryContent.Text((formValue.value as JsonPrimitive).content)
                                    }
                                     FormFieldType.Checkbox -> {
                                        StoryContent.Text((formValue.value as JsonPrimitive).boolean.let {
                                            if (it) {
                                                // todo: translate
                                                "Checked"
                                            } else {
                                                // todo: translate
                                                "Not checked"
                                            }
                                        })
                                     }

                                    FormFieldType.Photos -> {
                                        StoryContent.Photos(
                                            (formValue.value as JsonArray).map {
                                                (it as JsonPrimitive).content
                                            }
                                        )
                                    }
                                    else -> StoryContent.Text("—")
                                }
                            )
                        }.flatten()).toJsonStoryContent(json)

                        val submission = Card(
                            parent = data.page!!,
                            person = formOwner,
                            // todo: translate
                            name = submitter?.let { "Submission from ${submitter.name ?: "Someone"}" } ?: "Anonymous submission",
                            content = content
                        ).let {
                            db.insert(it)
                        }

                        data.groups?.forEach { groupId ->
                            // Verify form owner is a member of the destination group
                            val member = db.member(formOwner, groupId)

                            if (member != null) {
                                Message(
                                    text = submission.name,
                                    attachment = json.encodeToString(CardAttachment(submission.id!!)),
                                    group = member.to!!
                                )
                            }
                        }

                        // Success
                        RunWidgetResponse(data = null)
                    }
                    else -> HttpStatusCode.BadRequest.description("Widget cannot be run")
                }
            }
        }
    }

    authenticate {
        post("/widgets") {
            respond {
                val widget = call.receive<CreateWidgetBody>()
                db.insert(Widget(
                    person = me.id!!,
                    widget = widget.widget,
                    data = widget.data
                ))
            }
        }

        post("/widgets/{id}") {
            respond {
                val widget = db.document(Widget::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                val update = call.receive<Widget>()

                if (update.data != null) {
                    widget.data = update.data
                }

                db.update(widget)
            }
        }
    }
}
