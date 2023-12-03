package com.queatz.api

import com.queatz.db.CreateWidgetBody
import com.queatz.db.Widget
import com.queatz.db.widget
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*


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
                val widget = db.widget(me.id!!, parameter("id"))
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
