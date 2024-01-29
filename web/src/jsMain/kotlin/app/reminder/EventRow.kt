package app.reminder

import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.deleteReminderOccurrence
import app.ailaai.api.updateReminderOccurrence
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.Menu
import app.page.*
import com.queatz.db.ReminderOccurrence
import components.IconButton
import focusable
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinInstant
import lib.format
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import parseDateTime

@Composable
fun EventRow(
    view: ScheduleView,
    event: ReminderEvent,
    onUpdate: () -> Unit,
    onOpenReminder: () -> Unit,
    showOpen: Boolean = true
) {
    val scope = rememberCoroutineScope()

    var menuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }

    fun markAsDone(done: Boolean) {
        scope.launch {
            api.updateReminderOccurrence(event.reminder.id!!, event.updateDate, ReminderOccurrence(
                done = done
            )) {
                onUpdate()
            }
        }
    }

    fun edit() {
        scope.launch {
            // todo translate
            val note = inputDialog(
                "Edit note",
                "",
                confirmButton = "Update",
                defaultValue = event.occurrence?.note?.notBlank ?: event.reminder.note?.notBlank ?: ""
            )

            if (note == null) return@launch

            api.updateReminderOccurrence(
                event.reminder.id!!, event.updateDate, ReminderOccurrence(
                note = note
            )) {
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
                    date,
                    time,
                    { date = it },
                    { time = it },
                )
            }

            if (result != true) return@launch

            api.updateReminderOccurrence(
                event.reminder.id!!,
                event.updateDate,
                ReminderOccurrence(
                    date = parseDateTime(date, time, event.date).toKotlinInstant()
                )
            ) {
                onUpdate()
            }
        }
    }

    menuTarget?.let { target ->
        Menu({ menuTarget = null }, target) {
            if (showOpen) {
                // todo: translate
                item("Open") {
                    onOpenReminder()
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

    val eventType = event.event
    val date = event.date
    val done = event.occurrence?.done ?: false
    // todo: translate
    val text = event.reminder.title?.notBlank ?: "New reminder"
    val note = event.occurrence?.note?.notBlank ?: event.reminder.note?.notBlank ?: ""

    Div({
        classes(SchedulePageStyles.row)

        // todo: translate
        title("Mark as done")

        onClick {
            markAsDone(!done)
        }

        focusable()
    }
    ) {
        Div({
            style {
                flex(1)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
            }
        }) {
            Div({
                classes(SchedulePageStyles.rowText)
                style {
                    if (done) {
                        opacity(.5)
                        textDecoration("line-through")
                    }
                }
            }) {
                Text(
                    text + when (eventType) {
                        ReminderEventType.Start -> " starts"
                        ReminderEventType.End -> " ends"
                        ReminderEventType.Occur -> ""
                    }
                )
            }
            Div({
                style {
                    color(Styles.colors.secondary)
                    fontSize(14.px)
                    whiteSpace("pre-wrap")

                    if (done) {
                        textDecoration("line-through")
                        opacity(.5)
                    }
                }
            }) {
                val details = note.notBlank?.let { " • $it" } ?: ""

                when (view) {
                    ScheduleView.Daily -> {
                        Text("${format(date, "h:mm a")}$details")
                    }

                    ScheduleView.Weekly -> {
                        Text("${format(date, "MMMM do, EEEE, h:mm a")}$details")
                    }

                    ScheduleView.Monthly -> {
                        Text("${format(date, "do, EEEE, h:mm a")}$details")
                    }

                    ScheduleView.Yearly -> {
                        Text("${format(date, "MMMM do, EEEE, h:mm a")}$details")
                    }
                }
            }
        }
        Div({
            classes(SchedulePageStyles.rowActions)
        }) {
            // todo: translate
            IconButton("edit", "Edit", styles = {
            }) {
                it.stopPropagation()
                edit()
            }
            // todo: translate
            IconButton("more_vert", "Options", styles = {
            }) {
                it.stopPropagation()
                menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
            }
        }
    }
}
