package app.reminder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.deleteReminderOccurrence
import app.ailaai.api.updateReminderOccurrence
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.Menu
import app.page.ReminderEvent
import app.page.updateDate
import appString
import application
import com.queatz.db.ReminderOccurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinInstant
import time.format
import notBlank
import org.w3c.dom.DOMRect
import parseDateTime

@Composable
fun EventMenu(
    onDismissRequest: () -> Unit,
    scope: CoroutineScope,
    event: ReminderEvent,
    menuTarget: DOMRect,
    showMarkAsDone: Boolean = false,
    showEditNote: Boolean = false,
    showOpen: Boolean = false,
    onUpdate: () -> Unit,
    onOpen: () -> Unit,
) {
    fun markAsDone(done: Boolean) {
        scope.launch {
            api.updateReminderOccurrence(event.reminder.id!!, event.updateDate, ReminderOccurrence(
                done = done
            )
            ) {
                onUpdate()
            }
        }
    }

    fun edit() {
        scope.launch {
            val note = inputDialog(
                title = application.appString { editNote },
                confirmButton = application.appString { update },
                defaultValue = event.occurrence?.note?.notBlank ?: event.reminder.note?.notBlank ?: ""
            )

            if (note == null) return@launch

            api.updateReminderOccurrence(
                id = event.reminder.id!!,
                occurrence = event.updateDate,
                update = ReminderOccurrence(
                    note = note
                )
            ) {
                onUpdate()
            }
        }
    }

    fun delete() {
        scope.launch {
            val result = dialog(
                title = application.appString { deleteOccurrence },
                confirmButton = application.appString { yesDelete }
            )

            if (result != true) return@launch

            api.deleteReminderOccurrence(
                event.reminder.id!!,
                event.occurrence?.occurrence ?: event.date.toKotlinInstant()
            ) {
                onUpdate()
            }
        }
    }

    fun reschedule() {
        scope.launch {
            var date by mutableStateOf(format(event.date, "yyyy-MM-dd"))
            var time by mutableStateOf(format(event.date, "HH:mm"))
            val result = dialog(
                title = application.appString { rescheduleOccurrence },
                confirmButton = application.appString { update }
            ) {
                ReminderDateTime(
                    date = date,
                    time = time,
                    onDate = { date = it },
                    onTime = { time = it },
                )
            }

            if (result != true) return@launch

            api.updateReminderOccurrence(
                id = event.reminder.id!!,
                occurrence = event.updateDate,
                update = ReminderOccurrence(
                    date = parseDateTime(date, time, event.date).toKotlinInstant()
                )
            ) {
                onUpdate()
            }
        }
    }

    menuTarget.let { target ->
        Menu(onDismissRequest, target) {
            if (showMarkAsDone) {
                val done = event.occurrence?.done == true
                item(
                    title = if (done) {
                        appString { unmarkAsDone }
                    } else {
                        appString { markAsDone }
                    }
                ) {
                    markAsDone(!done)
                }
            }
            if (showEditNote) {
                item(appString { editNote }) {
                    edit()
                }
            }
            if (showOpen) {
                item(appString { open }) {
                    onOpen()
                }
            }
            item(appString { reschedule }) {
                reschedule()
            }
            item(appString { delete }) {
                delete()
            }
        }
    }
}
