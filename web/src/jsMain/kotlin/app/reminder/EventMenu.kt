package app.reminder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.deleteReminderOccurrence
import app.ailaai.api.updateReminderOccurrence
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.Menu
import app.page.ReminderEvent
import app.page.updateDate
import com.queatz.db.ReminderOccurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinInstant
import lib.format
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
                // todo translate
                title = "Edit note",
                placeholder = "",
                // todo translate
                confirmButton = "Update",
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
            // todo translate
            val result = dialog("Delete this occurrence?", confirmButton = "Yes, delete")

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
            // todo translate
            val result = dialog("Reschedule occurrence", confirmButton = "Update") {
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
                // todo: translate
                item(if (done) "Unmark as done" else "Mark as done") {
                    markAsDone(!done)
                }
            }
            if (showEditNote) {
                // todo: translate
                item("Edit note") {
                    edit()
                }
            }
            if (showOpen) {
                // todo: translate
                item("Open") {
                    onOpen()
                }
            }
            // todo: translate
            item("Reschedule") {
                reschedule()
            }
            // todo: translate
            item("Delete") {
                delete()
            }
        }
    }
}
