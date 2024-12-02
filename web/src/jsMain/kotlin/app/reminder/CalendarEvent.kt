package app.reminder

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
        event.date.formatSecondary(view),
        event.reminder.categories?.firstOrNull(),
        event.occurrence?.note?.notBlank ?: event.reminder.note?.notBlank
    )
    val done = event.occurrence?.done ?: false

    Div({
        classes(Styles.calendarEvent)

        style {
            // todo: reminder duration
            height(1.r)
            minHeight(1.r)
            maxHeight(1.r)

           if (done) {
               textDecoration("line-through")
           }
        }

        title(
            "$titleString\n\n$info"
        )

        onClick {
            it.stopPropagation()
            menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
        }
    }) {
        Span({
            if (done) {
                style {
                    opacity(.5)
                }
            }
        }) {
            // todo: translate
            Text(titleString)
        }
    }
}
