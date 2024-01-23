import com.queatz.db.Reminder
import com.queatz.db.ReminderOccurrence
import com.queatz.db.ReminderOccurrences
import kotlinx.datetime.Instant

enum class ReminderEventType {
    Start,
    Occur,
    End
}

data class ReminderEvent(
    /**
     * The reminder that spawned this event
     */
    val reminder: Reminder,
    /**
     * The date of the event
     */
    val date: Instant,
    /**
     * The type of event.
     */
    val event: ReminderEventType,
    /**
     * The `ReminderOccurrence` associated with this event, if any.
     */
    val occurrence: ReminderOccurrence?
)

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
                        it.reminder.start!!,
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
                            it.reminder.end!!,
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
                        (occurrence.date ?: occurrence.occurrence)!!,
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
                    date,
                    ReminderEventType.Occur,
                    null
                )
            )
        }
    }
}.sortedBy { it.date }
