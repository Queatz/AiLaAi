package app.reminder

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.updateReminderOccurrence
import app.dialog.inputDialog
import app.page.ReminderEvent
import app.page.ReminderEventType
import app.page.SchedulePageStyles
import app.page.ScheduleView
import app.page.updateDate
import bulletedString
import com.queatz.db.ReminderOccurrence
import components.IconButton
import focusable
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textDecoration
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement

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

    menuTarget?.let {
        EventMenu(
            onDismissRequest = {
                menuTarget = null
            },
            scope = scope,
            showOpen = showOpen,
            event = event,
            menuTarget = it,
            onUpdate = onUpdate,
            onOpen = onOpenReminder
        )
    }

    val eventType = event.event
    val date = event.date
    val done = event.occurrence?.done ?: false
    // todo: translate
    val text = event.reminder.title?.notBlank ?: "New reminder"

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
                property("word-break", "break-word")
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
                Text(
                    bulletedString(
                        date.formatSecondary(view).let {
                            val duration = (event.occurrence?.duration ?: event.reminder.duration)?.let {
                                it.formatDuration()
                            }
                            if (duration == null) {
                                it
                            } else {
                                "$it ($duration)"
                            }
                        },
                        event.reminder.categories?.firstOrNull(),
                        event.occurrence?.note?.notBlank ?: event.reminder.note?.notBlank
                    )
                )
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
