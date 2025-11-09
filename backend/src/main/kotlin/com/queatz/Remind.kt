package com.queatz

import ReminderEvent
import com.queatz.db.ReminderOccurrence
import com.queatz.db.ReminderStickiness
import com.queatz.db.occurrences
import com.queatz.plugins.db
import com.queatz.plugins.notify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import toEvents
import java.util.logging.Logger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

val remind = Remind()

/**
 * Manages reminder scheduling and notification processing.
 * Handles periodic checks for upcoming reminders and processes sticky reminders
 * for recurring notifications.
 */
class Remind {
    private lateinit var scope: CoroutineScope

    /**
     * Starts the reminder processing service.
     * Initializes periodic checks for reminders every minute and processes
     * any reminders that need to be triggered.
     *
     * @param scope The coroutine scope to launch reminder processing jobs
     */
    fun start(scope: CoroutineScope) {
        this.scope = scope

        val minute = MutableStateFlow((Clock.System.now() - 1.minutes).startOfMinute())

        scope.launch {
            while (true) {
                delayUntilNextMinute()
                minute.update { Clock.System.now().startOfMinute() }
            }
        }

        scope.launch {
            minute.collect { minute ->
                Logger.getAnonymousLogger().info("REMIND minute=$minute")
                pushReminders(
                    db.occurrences(null, minute, minute).toEvents().filter {
                        it.date >= minute && it.date < minute + 1.minutes
                    }
                )
            }
        }
    }

    /**
     * Processes and sends notifications for reminder events.
     * Updates sticky reminders by creating or updating their occurrences
     * for the next scheduled time.
     *
     * @param events List of reminder events to process
     */
    private fun pushReminders(events: List<ReminderEvent>) {
        Logger.getAnonymousLogger().info("REMIND events=${events.size}")
        events.forEach {
            Logger.getAnonymousLogger().info("REMIND reminder=${it.reminder.title} date=${it.date} event=${it.event}")
            notify.reminder(
                date = it.date,
                reminder = it.reminder,
                occurrence = it.occurrence
            )

            updateStickyReminder(it)?.let { newDate ->
                Logger.getAnonymousLogger().info("REMIND [Sticky] ${it.reminder.title} stickiness=${it.reminder.stickiness} to=$newDate")
                if (it.occurrence == null) {
                    db.insert(
                        ReminderOccurrence(
                            reminder = it.reminder.id,
                            occurrence = it.date,
                            date = newDate
                        )
                    )
                } else {
                    db.update(
                        it.occurrence!!.apply {
                            date = newDate
                        }
                    )
                }
            }
        }
    }
}


/**
 * Calculates the next occurrence time for a sticky reminder.
 * Handles different stickiness types (Hourly, Daily, Weekly, Monthly, Yearly)
 * and takes into account timezone offsets.
 *
 * @param event The reminder event to process
 * @return The next occurrence time as Instant, or null if the reminder shouldn't be repeated
 */
fun updateStickyReminder(event: ReminderEvent): Instant? {
    val (reminder, date, _, occurrence) = event

    // Skip reminders without stickiness or with None stickiness
    if (reminder.stickiness == null || reminder.stickiness == ReminderStickiness.None) {
        return null
    }

    // Skip occurrences that are marked as done
    if (occurrence?.done == true) {
        return null
    }

    // Get the timezone for this reminder
    val utcOffset: UtcOffset = reminder.utcOffset?.let {
        val hours = it.hours.inWholeHours.toInt()
        UtcOffset(
            hours = hours,
            minutes = (it - hours).hours.inWholeMinutes.toInt()
        )
    } ?: UtcOffset.ZERO

    // Get the local date and time for the occurrence
    val localDateTime = date.toLocalDateTime(utcOffset.asTimeZone())

    // Calculate the new date based on stickiness
    val newDate = when (reminder.stickiness) {
        ReminderStickiness.Hourly -> {
            // Push to the next hour
            date.plus(1.hours)
        }
        ReminderStickiness.Daily -> {
            // Push to the next day
            date.plus(1.days)
        }
        ReminderStickiness.Weekly -> {
            // Push to the next week
            date.plus(7.days)
        }
        ReminderStickiness.Monthly -> {
            // Push to the next month
            val currentYear = localDateTime.year
            val currentMonth = localDateTime.monthNumber // 1..12
            val nextMonth = if (currentMonth == 12) 1 else currentMonth + 1
            val nextYear = if (currentMonth == 12) currentYear + 1 else currentYear
            val maxDay = daysInMonth(nextYear, nextMonth)
            val nextDay = minOf(localDateTime.dayOfMonth, maxDay)
            LocalDateTime(
                nextYear,
                nextMonth,
                nextDay,
                localDateTime.hour,
                localDateTime.minute,
                localDateTime.second,
                localDateTime.nanosecond
            ).toInstant(utcOffset)
        }
        ReminderStickiness.Yearly -> {
            // Push to the next year
            val nextYear = localDateTime.year + 1
            val maxDay = daysInMonth(nextYear, localDateTime.monthNumber)
            val nextDay = minOf(localDateTime.dayOfMonth, maxDay)
            LocalDateTime(
                nextYear,
                localDateTime.monthNumber,
                nextDay,
                localDateTime.hour,
                localDateTime.minute,
                localDateTime.second,
                localDateTime.nanosecond
            ).toInstant(utcOffset)
        }
        else -> date
    }

    return newDate
}

/**
 * Extension function to check if a year is a leap year.
 */
fun Int.isLeap(): Boolean {
    return this % 4 == 0 && (this % 100 != 0 || this % 400 == 0)
}


fun daysInMonth(year: Int, monthNumber: Int): Int {
    return when (monthNumber) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year.isLeap()) 29 else 28
        else -> throw IllegalArgumentException("Invalid monthNumber: $monthNumber")
    }
}
