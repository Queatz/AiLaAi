package app.reminder

import androidx.compose.runtime.*
import api
import app.ailaai.api.reminderOccurrences
import app.page.ReminderEvent
import app.page.SchedulePageStyles
import app.page.ScheduleView
import com.queatz.db.Reminder
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun ReminderEvents(reminder: Reminder) {
    val scope = rememberCoroutineScope()

    var events by remember(reminder) {
        mutableStateOf(emptyList<ReminderEvent>())
    }

    suspend fun reload() {
        api.reminderOccurrences(
            reminder.id!!,
            start = reminder.start!!,
            end = reminder.end ?: Clock.System.now()
        ) {
            events = it.toEvents().asReversed()
        }
    }

    LaunchedEffect(reminder) {
        reload()
    }

    if (events.isNotEmpty()) {
        Div({
            style {
                margin(1.r)
            }
        }) {
            Div({
                classes(SchedulePageStyles.title)
            }) {
                Text("History")
            }
            Div({
                classes(SchedulePageStyles.section)
            }) {
                events.forEach { event ->
                    EventRow(
                        ScheduleView.Yearly,
                        event,
                        showOpen = false,
                        onUpdate = {
                             scope.launch {
                                 reload()
                             }
                        },
                        onOpenReminder = {
                        }
                    )
                }
            }
        }
    }
}
