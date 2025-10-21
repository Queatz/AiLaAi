package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import com.queatz.startOfSecond
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.toInstant

fun Route.reminderRoutes() {
    authenticate(optional = true) {
        get("/reminders/{id}") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (reminder.isVisibleToMe(meOrNull)) {
                    reminder
                } else {
                    HttpStatusCode.NotFound
                }
            }
        }

        get("/occurrences") {
            respond {
                val start = call.parameters["start"]?.toInstant()
                    ?: return@respond HttpStatusCode.BadRequest.description("Missing 'start' parameter")
                val end = call.parameters["end"]?.toInstant()
                    ?: return@respond HttpStatusCode.BadRequest.description("Missing 'end' parameter")
                val geo = call.parameters["geo"]?.split(",")?.map { it.toDouble() }?.takeIf { it.size == 2 }
                val open = call.parameters["open"]?.toBoolean()?.takeIf { it }

                db.occurrences(
                    person = meOrNull?.id,
                    start = start,
                    end = end,
                    geo = geo,
                    open = open
                )
            }
        }
    }

    authenticate {
        get("/reminders") {
            respond {
                db.reminders(
                    person = me.id!!,
                    offset = call.parameters["offset"]?.toInt() ?: 0,
                    limit = call.parameters["limit"]?.toInt() ?: 100
                )
            }
        }

        post("/reminders") {
            respond {
                val new = call.receive<Reminder>()

//                todo val myMember = db.member(group.id!!, me.id!!) ?: return@forEach
//                todo check that I'm friends with everyone one in .people

                new.title ?: return@respond HttpStatusCode.BadRequest.description("Missing 'title' parameter")
                new.start ?: return@respond HttpStatusCode.BadRequest.description("Missing 'start' parameter")
                new.timezone ?: return@respond HttpStatusCode.BadRequest.description("Missing 'timezone' parameter")
                new.utcOffset ?: return@respond HttpStatusCode.BadRequest.description("Missing 'utcOffset' parameter")

                db.insert(
                    Reminder(
                        person = me.id!!,
                        groups = new.groups,
                        people = new.people,
                        open = new.open,
                        geo = new.geo,
                        attachment = new.attachment,
                        title = new.title,
                        note = new.note,
                        start = new.start?.startOfSecond(),
                        end = new.end?.startOfSecond(),
                        timezone = new.timezone,
                        utcOffset = new.utcOffset ?: 0.0,
                        schedule = new.schedule,
                        stickiness = new.stickiness
                    )
                )
            }
        }

        get("/reminders/{id}/occurrences") {
            respond {
                val start = call.parameters["start"]?.toInstant() ?: return@respond HttpStatusCode.BadRequest.description("Missing 'start' parameter")
                val end = call.parameters["end"]?.toInstant() ?: return@respond HttpStatusCode.BadRequest.description("Missing 'end' parameter")

                db.occurrences(
                    person = me.id!!,
                    start = start,
                    end = end,
                    reminders = listOf(parameter("id"))
                )
            }
        }

        post("/reminders/{id}") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (reminder.isMine(me)) {
                    val update = call.receive<Reminder>()

                    if (update.title != null) {
                        reminder.title = update.title
                    }

                    if (update.note != null) {
                        reminder.note = update.note
                    }

                    if (update.open != null) {
                        reminder.open = update.open
                    }

                    if (update.geo != null) {
                        reminder.geo = update.geo?.takeIf { it.size == 2 }
                    }

                    if (update.groups != null) {
                        reminder.groups = update.groups
                    }

                    if (update.people != null) {
                        reminder.people = update.people
                    }

                    if (update.categories != null) {
                        reminder.categories = update.categories
                    }

                    if (update.attachment != null) {
                        reminder.attachment = update.attachment
                    }

                    if (update.stickiness != null) {
                        reminder.stickiness = update.stickiness
                    }

                    if (update.start != null) {
                        reminder.start = update.start?.startOfSecond()
                    }

                    if (update.alarm != null) {
                        reminder.alarm = update.alarm
                    }

                    if (update.timezone != null) {
                        reminder.timezone = update.timezone
                    }

                    if (update.utcOffset != null) {
                        reminder.utcOffset = update.utcOffset
                    }

                    if (update.duration != null) {
                        reminder.duration = update.duration
                    }

                    if (update.photo != null) {
                        reminder.photo = update.photo
                    }

                    if (update.content != null) {
                        reminder.content = update.content
                    }

                    // TODO need a way for the user to delete
                    if (update.end != null) {
                        reminder.end = update.end?.startOfSecond()
                    }

                    // TODO need a way for the user to delete
                    if (update.schedule != null) {
                        reminder.schedule = update.schedule
                    }

                    // todo notify other people on this reminder that it has been changed

                    db.update(reminder)
                } else {
                    HttpStatusCode.NotFound
                }
            }
        }
        
        post("reminders/{id}/join") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (!reminder.isVisibleToMe(meOrNull)) {
                    return@respond HttpStatusCode.NotFound
                }

                val currentPeople = reminder.people ?: emptyList()
                if (currentPeople.contains(me.id)) {
                    return@respond HttpStatusCode.BadRequest.description("You are already in this reminder")
                }

                reminder.people = currentPeople + me.id!!
                db.update(reminder)
                HttpStatusCode.OK
            }
        }
        
        post("reminders/{id}/leave") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (!reminder.isVisibleToMe(meOrNull)) {
                    return@respond HttpStatusCode.NotFound
                }

                val currentPeople = reminder.people ?: emptyList()
                if (!currentPeople.contains(me.id)) {
                    return@respond HttpStatusCode.BadRequest.description("You are not in this reminder")
                }

                reminder.people = currentPeople - me.id!!
                db.update(reminder)
                HttpStatusCode.OK
            }
        }

        post("/reminders/{id}/occurrences/{occurrence}") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (!reminder.isMine(me)) {
                    return@respond HttpStatusCode.NotFound
                }

                val at = parameter("occurrence").toInstant().startOfSecond()
                val occurrence = db.occurrence(reminder.id!!, at) ?: ReminderOccurrence(
                    reminder = reminder.id!!,
                    occurrence = at,
                    date = at
                ).let(db::insert)

                val occurrenceUpdate = call.receive<ReminderOccurrence>()

                if (occurrenceUpdate.date != null) {
                    occurrence.date = occurrenceUpdate.date?.startOfSecond()
                }

                if (occurrenceUpdate.note != null) {
                    occurrence.note = occurrenceUpdate.note
                }

                if (occurrenceUpdate.done != null) {
                    occurrence.done = occurrenceUpdate.done
                }

                if (occurrenceUpdate.duration != null) {
                    occurrence.duration = occurrenceUpdate.duration
                }

                db.update(occurrence)
            }
        }

        post("/reminders/{id}/delete") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (!reminder.isMine(me)) {
                    return@respond HttpStatusCode.NotFound
                }

                db.deleteReminderOccurrences(reminder.id!!)
                db.delete(reminder)

                HttpStatusCode.NoContent
            }
        }

        post("/reminders/{id}/occurrences/{date}/delete") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (!reminder.isMine(me)) {
                    return@respond HttpStatusCode.NotFound
                }

                db.upsertReminderOccurrenceGone(reminder.id!!, parameter("date").toInstant().startOfSecond(), true)

                HttpStatusCode.NoContent
            }
        }
    }
}

private fun Reminder.isMine(me: Person) = person == me.id
private fun Reminder.isMineOrJoined(me: Person) = person == me.id || me.id in (people ?: emptyList())

private fun Reminder.isVisibleToMe(me: Person?) = open == true || (me != null && isMineOrJoined(me))
