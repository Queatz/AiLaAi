package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlin.time.Clock
import kotlinx.serialization.encodeToString

fun Route.wildRoutes() {
    authenticate(optional = true) {
        post("/wild/reply") {
            respond {
                val wildReply = call.receive<WildReplyBody>()

                if (wildReply.device.isBlank()) {
                    return@respond HttpStatusCode.BadRequest.description("Missing 'device'")
                }

                val card = db.document(Card::class, wildReply.card)
                    ?: return@respond HttpStatusCode.NotFound.description("Card not found")
                var wildDevice = db.device(DeviceType.Web, wildReply.device)
                val wildPerson: Person = meOrNull ?: if (wildDevice.person == null) {
                    // todo: translate
                    db.insert(Person(source = PersonSource.Web, name = "Web message", seen = Clock.System.now())).also {
                        wildDevice.person = it.id
                        wildDevice = db.update(wildDevice)
                    }
                } else {
                    db.document(Person::class, wildDevice.person!!)
                        ?: return@respond HttpStatusCode.BadRequest.description("Person not found")
                }

                val people = listOf(wildPerson.id!!, card.person!!).distinct()
                val group = app.createGroup(people).let {
                    if (meOrNull == null) {
                        // todo: translate
                        it.description =
                            "This is a message from the web. The other person may not see your replies, but you can jot down notes here."
                    }
                    db.update(it)
                }
                val wildMember = db.member(wildPerson.id!!, group.id!!)
                    ?: return@respond HttpStatusCode.NotFound.description("Member not found")

                wildMember.seen = Clock.System.now()
                db.update(wildMember)
                wildPerson.seen = Clock.System.now()
                db.update(wildPerson)
                group.seen = Clock.System.now()
                db.update(group)

                val attachment = json.encodeToString(CardAttachment(card.id!!))
                // Insert card
                db.insert(Message(group.id, wildMember.id, wildReply.conversation, attachment))
                // Insert reply
                db.insert(Message(group.id, wildMember.id, wildReply.message))

                // todo maybe also send a notification for the card message

                notify.message(
                    group = group,
                    person = wildPerson,
                    message = Message(text = wildReply.message.ellipsize())
                )

                HttpStatusCode.OK
            }
        }
    }
}
