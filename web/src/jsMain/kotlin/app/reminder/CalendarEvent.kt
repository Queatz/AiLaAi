package app.reminder

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.page.ReminderEvent
import app.page.ScheduleView
import bulletedString
import notBlank
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.textDecoration
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import kotlin.time.Duration.Companion.minutes

@Composable
fun CalendarEvent(
    event: ReminderEvent,
    view: ScheduleView,
    millisecondsIn1Rem: Double,
    onUpdate: () -> Unit,
    onOpen: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var menuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }
    menuTarget?.let {
        EventMenu(
            onDismissRequest = {
                menuTarget = null
            },
            scope = scope,
            showOpen = true,
            showEditNote = true,
            showMarkAsDone = true,
            event = event,
            menuTarget = it,
            onUpdate = onUpdate,
            onOpen = onOpen
        )
    }

    val titleString = event.reminder.title ?: "New reminder"
    val info = bulletedString(
        titleString,
        event.date.formatSecondary(view).let {
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
    val done = event.occurrence?.done ?: false
    val duration = (event.occurrence?.duration ?: event.reminder.duration ?: 0)

    Div({
        classes(Styles.calendarEvent)

        style {
            val h = ((duration / millisecondsIn1Rem)).coerceAtLeast(1.0).r
            height(h)
            minHeight(h)
            maxHeight(h)

            if (done) {
                textDecoration("line-through")
            }
        }

        title(
            info
        )

        onClick {
            it.stopPropagation()
            menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
        }
    }) {
        Span({
            style {
                if (done) {
                    opacity(.5)
                }
                whiteSpace("pre-wrap")
            }
        }) {
            Text(info)
        }
    }
}
