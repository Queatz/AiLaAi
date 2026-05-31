package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.app
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.notify
import com.queatz.plugins.respond
import com.queatz.push.*
import com.queatz.plugins.openAi
import io.ktor.http.*
import java.io.File
import java.util.logging.Logger
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaInstant

fun Route.signalRoutes() {
    authenticate {
        get("signals") {
            val localHour = call.parameters["localHour"]?.toIntOrNull() ?: run {
                val now = Clock.System.now().toJavaInstant().atZone(java.time.ZoneOffset.UTC)
                ((now.hour + (me.utcOffset ?: 0.0).toInt()) % 24 + 24) % 24
            }
            val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
            respond { db.signals(localHour, offset) }
        }

        post("signals") {
            val signal = call.receive<Signal>()
            respond { db.insert(signal) }
        }

        get("signals/me") {
            respond { db.personSignals(me.id!!) }
        }

        post("signals/{id}/toggle") {
            val signalId = call.parameters["id"]!!
            respond { db.toggleSignal(me.id!!, signalId)!! }
        }

        get("signals/active") {
            val person = me
            val personId = person.id!!
            val geo = call.parameters["geo"]?.split(",")?.mapNotNull { it.toDoubleOrNull() } ?: person.geo
            Logger.getAnonymousLogger().info("[SIGNALS] activeSignals for person $personId, geo $geo")
            
            respond {
                val signals = db.activeSignals(personId, geo)
                Logger.getAnonymousLogger().info("[SIGNALS] activeSignals found ${signals.size} signals for person $personId")
                signals.forEach { s ->
                    Logger.getAnonymousLogger().info("[SIGNALS] signal ${s.signalSend.id} (person: ${s.signalSend.person}) has ${s.replies?.size ?: 0} replies")
                    s.replies?.forEach { r ->
                        Logger.getAnonymousLogger().info("[SIGNALS]   reply ${r.signalReply.id} (from: ${r.signalReply.person}) has person object: ${r.person != null} (id: ${r.person?.id}, name: ${r.person?.name})")
                    }
                }
                
                val mine = signals.filter { it.signalSend.person?.split("/")?.last() == person.id?.split("/")?.last() }
                val others = signals.filter { it.signalSend.person?.split("/")?.last() != person.id?.split("/")?.last() }
                
                Logger.getAnonymousLogger().info("[SIGNALS] activeSignals for person $personId: mine=${mine.size} (${mine.map { it.signalSend.id }}), others=${others.size} (${others.map { it.signalSend.id }}), all persons=${signals.map { it.signalSend.person }}")
                
                ActiveSignalsResponse(mine, others)
            }
        }

        post("signals/send") {
            val personId = me.id!!
            val body = call.receive<SendSignalBody>()
            val now = Clock.System.now()
            
            respond {
                val signalSend = SignalSend(
                    person = personId,
                    signal = body.signal,
                    message = body.message,
                    photo = body.photo,
                    audio = body.audio,
                    geo = body.geo,
                    radius = body.radius,
                    audience = body.audience,
                    groups = body.groups,
                    expiry = now + body.duration.milliseconds
                ).let { db.insert(it) }
                
                Logger.getAnonymousLogger().info("[SIGNALS] sending signal from $personId: ${signalSend.id} (audience: ${body.audience})")
                
                val sender = me
                val signal = db.document(Signal::class, body.signal)!!

                // Increment popularity
                val localHour = body.localHour ?: run {
                    val zdt = now.toJavaInstant().atZone(java.time.ZoneOffset.UTC)
                    ((zdt.hour + (me.utcOffset ?: 0.0).toInt()) % 24 + 24) % 24
                }
                db.incrementSignalStats(body.signal, localHour)
                
                val friends = db.friends(personId)
                val nearby = if (body.geo != null && body.radius != null) {
                     db.peopleNearby(body.geo!!, body.radius!! * 1000.0).map { it.id!! }.filter { it != personId }
                } else emptyList()

                val groups = if (body.groups != null) {
                    db.peopleInGroups(body.groups.orEmpty())
                } else emptyList()

                val potentialNotifyList = when (body.audience) {
                    SignalAudience.Nearby -> nearby
                    SignalAudience.Friends -> friends
                    SignalAudience.Groups -> groups
                    null -> (friends + nearby + groups).distinct()
                }.filter { it != personId }
                
                val notifyList = db.peopleWithSignalOn(potentialNotifyList, body.signal)
                
                Logger.getAnonymousLogger().info("[SIGNALS] notifyList for signal ${signalSend.id}: $notifyList")
                
                val transcription = body.audio?.let { audioPath ->
                    try {
                        val file = File(".$audioPath")
                        if (file.exists()) {
                            openAi.transcribe(file.readBytes())
                        } else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                
                notify.notifyPeople(
                    notifyList,
                    PushData(
                        action = PushAction.Signal,
                        data = SignalPushData(sender, signal, signalSend, transcription)
                    )
                )
                
                signalSend
            }
        }

        post("signals/send/{id}/cancel") {
            val personId = me.id!!
            val id = call.parameters["id"]!!
            respond {
                db.cancelSignalSend(id, personId) ?: run {
                    call.respond(HttpStatusCode.NotFound)
                    SignalSend()
                }
            }
        }

        post("signals/send/{id}/reply") {
            val personId = me.id!!
            val id = call.parameters["id"]!!
            val body = call.receive<SignalReplyBody>()
            
            respond {
                val signalSend = db.document(SignalSend::class, id)
                if (signalSend == null || signalSend.expiry!! <= Clock.System.now()) {
                    call.respond(HttpStatusCode.Gone)
                    return@respond SignalReply()
                }

                if (db.signalReply(personId, id) != null) {
                    call.respond(HttpStatusCode.Conflict)
                    return@respond SignalReply()
                }
                
                val reply = SignalReply(
                    signalSend = id,
                    person = personId,
                    message = body.message,
                    photo = body.photo,
                    audio = body.audio,
                    released = false
                ).let { db.insert(it) }
                
                Logger.getAnonymousLogger().info("[SIGNALS] reply to signal $id from $personId: ${reply.id}")
                
                reply
            }
        }

        post("signals/send/{id}/create-group") {
            val personId = me.id!!
            val id = call.parameters["id"]!!
            val body = call.receive<CreateGroupBody>()

            respond {
                val signalSend = db.document(SignalSend::class, id)!!
                if (signalSend.person != personId) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@respond Group()
                }

                val signal = db.document(Signal::class, signalSend.signal!!)!!

                val group = app.createGroup(
                    people = body.people + personId,
                    hosts = listOf(personId),
                    name = signal.name,
                    description = signalSend.message,
                    categories = signal.categories
                )

                db.cancelSignalSend(id, personId)

                val invitor = me
                body.people.forEach { pId ->
                    val person = db.document(Person::class, pId) ?: return@forEach
                    notify.notifyPeople(listOf(pId), PushData(
                        PushAction.Group,
                        GroupPushData(
                            person = Person(name = person.name).apply { this.id = person.id },
                            group = Group(name = group.name).apply { this.id = group.id },
                            event = GroupEvent.Join,
                            details = GroupEventData(
                                invitor = Person(name = invitor.name).apply { this.id = invitor.id }
                            )
                        )
                    ))
                }

                group
            }
        }
    }
}
