package app.reminder

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.page.ReminderDragData
import app.page.ReminderEvent
import app.page.ScheduleView
import app.page.updateDate
import appString
import bulletedString
import io.ktor.http.ContentType
import json
import notBlank
import org.jetbrains.compose.web.attributes.Draggable
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

@Composable
fun CalendarEvent(
    event: ReminderEvent,
    view: ScheduleView,
    millisecondsIn1Rem: Long,
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

    val titleString = event.reminder.title ?: appString { newReminder }
    val info = bulletedString(
        titleString,
        "⏰".takeIf { event.reminder.alarm == true },
        event.date.formatSecondary(view).let {
            (event.occurrence?.duration ?: event.reminder.duration)?.formatDuration()?.let { duration ->
                "$it ($duration)"
            } ?: it
        },
        event.reminder.categories?.firstOrNull(),
        event.occurrence?.note?.notBlank ?: event.reminder.note?.notBlank
    )
    val done = event.occurrence?.done == true
    val duration = (event.occurrence?.duration ?: event.reminder.duration ?: 0)
    var element by remember { mutableStateOf<HTMLElement?>(null) }

    Div({
        classes(Styles.calendarEvent)

        style {
            val h = ((duration.toDouble() / millisecondsIn1Rem.toDouble())).coerceAtLeast(1.0).r
            height(h)
            minHeight(h)
            maxHeight(h)

            if (done) {
                textDecoration("line-through")
            }
        }

        title(info)

        draggable(Draggable.True)

        onDragStart { dragEvent ->
            dragEvent.dataTransfer?.setData(
                format = ContentType.Application.Json.toString(),
                data = json.encodeToString(ReminderDragData(event.reminder.id!!, event.updateDate))
            )
        }

        ref {
            element = it

            onDispose {
                element = null
            }
        }

        onClick {
            it.stopPropagation()
            menuTarget = if (menuTarget == null) element?.getBoundingClientRect() else null
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
