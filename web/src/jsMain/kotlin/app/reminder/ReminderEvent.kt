package app.reminder

import app.page.ReminderEvent
import app.page.ReminderEventType
import com.queatz.db.ReminderOccurrences
import kotlin.js.Date

/**
 * Rules are:
 *
 * If reminder has a schedule -> Show occurrences
 * Else: Show start and end (if defined)
 */
fun List<ReminderOccurrences>.toEvents() = buildList {
    this@toEvents.forEach {
        if (it.reminder.schedule == null) {
            // Occurrences always override
            if (it.occurrences.none { occurrence -> occurrence.occurrence == it.reminder.start }) {
                add(
                    ReminderEvent(
                        it.reminder,
                        Date(it.reminder.start!!.toEpochMilliseconds()),
                        if (it.reminder.end == null) ReminderEventType.Occur else ReminderEventType.Start,
                        null
                    )
                )
            }
            if (it.reminder.end != null) {
                // Occurrences always override
                if (it.occurrences.none { occurrence -> occurrence.occurrence == it.reminder.end }) {
                    add(
                        ReminderEvent(
                            it.reminder,
                            Date(it.reminder.end!!.toEpochMilliseconds()),
                            ReminderEventType.End,
                            null
                        )
                    )
                }
            }
        }

        it.occurrences.forEach { occurrence ->
            if (occurrence.gone != true) {
                add(
                    ReminderEvent(
                        it.reminder,
                        Date((occurrence.date ?: occurrence.occurrence)!!.toEpochMilliseconds()),
                        when {
                            it.reminder.schedule == null && it.reminder.end != null && it.reminder.start == occurrence.occurrence -> ReminderEventType.Start
                            it.reminder.schedule == null && it.reminder.end != null && it.reminder.end == occurrence.occurrence -> ReminderEventType.End
                            else -> ReminderEventType.Occur
                        },
                        occurrence
                    )
                )
            }
        }

        it.dates.filter { date ->
            // Occurrences always override
            it.occurrences.none { it.occurrence == date }
        }.forEach { date ->
            add(
                ReminderEvent(
                    it.reminder,
                    Date(date.toEpochMilliseconds()),
                    ReminderEventType.Occur,
                    null
                )
            )
        }
    }
}.sortedBy { it.date.getTime() }
