package app.reminder

import Styles
import androidx.compose.runtime.Composable
import app.page.ReminderEvent
import app.page.SchedulePageStyles
import app.page.ScheduleView
import appString
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

@Composable
fun Period(
    view: ScheduleView,
    start: Date,
    end: Date,
    events: List<ReminderEvent>?,
    onUpdate: (ReminderEvent) -> Unit,
    onOpen: (ReminderEvent) -> Unit,
) {
    Div({
        classes(SchedulePageStyles.title)
    }) {
        Text(start.formatTitle(view))
    }
    Div({
        classes(SchedulePageStyles.section)
    }) {
        if (events.isNullOrEmpty()) {
            Div({
                style {
                    padding(.5.r)
                    color(Styles.colors.secondary)
                }
            }) {
                Text(
                    value = if (events == null) {
                        "${appString { loading }}…"
                    } else {
                        appString { noReminders }
                    }
                )
            }
        } else {
            events.forEach { event ->
                EventRow(
                    view = view,
                    event = event,
                    onUpdate = {
                        onUpdate(event)
                    },
                    onOpenReminder = {
                        onOpen(event)
                    }
                )
            }
        }
    }
}
