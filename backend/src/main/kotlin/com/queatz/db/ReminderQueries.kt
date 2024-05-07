package com.queatz.db

import kotlinx.datetime.Instant

fun Db.reminders(person: String, offset: Int = 0, limit: Int = 20) = list(
    Reminder::class,
    """
        for x in @@collection
            filter (x.${f(Reminder::person)} == @person or @person in x.${f(Reminder::people)})
            sort x.${f(Reminder::createdAt)} desc
            limit @offset, @limit
            return x
    """.trimIndent(),
    mapOf(
        "person" to person,
        "offset" to offset,
        "limit" to limit
    )
)

fun Db.occurrences(person: String?, start: Instant, end: Instant, reminders: List<String>? = null) = query(
    ReminderOccurrences::class,
    """
        let dayRangeStart = date_trunc(@start, 'd')
        let dayRange = range(0, date_diff(@start, @end, 'd'))
        for reminder in ${Reminder::class.collection()}
            filter (@person == null or reminder.${f(Reminder::person)} == @person or @person in reminder.${f(Reminder::people)})
                and (@reminders == null or reminder._key in @reminders)
                and reminder.${f(Reminder::start)} <= @end
                and (reminder.${f(Reminder::end)} == null or reminder.${f(Reminder::end)} >= @start)
            let utcOffset = reminder.${f(Reminder::utcOffset)} || 0.0
            let reminderMinute = date_minute(date_add(reminder.${f(Reminder::start)}, utcOffset, 'h'))
            let dates = reminder.${f(Reminder::schedule)} == null ? [] : (
                // Loop over days covered by @start until @end
                for offsetInDays in dayRange
                    // Start of UTC day
                    let date = date_add(dayRangeStart, offsetInDays, 'd')
                    // Local time at start of UTC day
                    let local = date_add(date, utcOffset, 'h')
                    // Start of local day in UTC time
                    let localStartOfDay = date_subtract(date, utcOffset, 'h')
                    // Below are all local representations (in reminder's timezone)
                    let dayOfWeek = date_dayofweek(local) + 1
                    let dayOfMonth = date_day(local)
                    let daysInMonth = date_days_in_month(local)
                    let week = floor((dayOfMonth - 1) / 7) + 1
                    let month = date_month(local)
                    let year = date_year(local)
                    filter (
                        // Occurs on any day
                        (count(reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::days)}) == 0 and count(reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::weekdays)}) == 0)
                        or (
                            // Occurs on days of month
                            (dayOfMonth in (reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::days)} || []))
                            or
                            // Occurs on last day of month
                            (dayOfMonth == daysInMonth and -1 in (reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::days)} || []))
                            or
                            // Occurs on days of week
                            dayOfWeek in (reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::weekdays)} || [])
                        )
                    ) and (
                        // Occurs in any week
                        count(reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::weeks)}) == 0
                        or
                        // Occurs in week of month
                        week in reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::weeks)}
                    ) and (
                        // Occurs in any month
                        count(reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::months)}) == 0
                        or
                        // Occurs in month of year
                        month in reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::months)}
                    ) and (
                        // Occurs in any year
                        count(reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::years)}) == 0
                        or
                        // Occurs in year
                        year in reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::years)}
                    )
                    for hour in (reminder.${f(Reminder::schedule)}.${f(ReminderSchedule::hours)} || [])
                        // Offset the start of the UTC day
                        let occurrenceDate = date_add(
                            date_add(localStartOfDay, hour, 'h'),
                            reminderMinute,
                            'minute'
                        )
                        // See https://github.com/arangodb/arangodb/issues/19747
                        filter occurrenceDate <= date_iso8601(@end) and occurrenceDate >= date_iso8601(@start)
                            and (
                                // See https://github.com/arangodb/arangodb/issues/19747
                                occurrenceDate >= date_iso8601(reminder.${f(Reminder::start)})
                                // See https://github.com/arangodb/arangodb/issues/19747
                                and (reminder.${f(Reminder::end)} == null or occurrenceDate <= date_iso8601(reminder.${f(Reminder::end)}))
                            )
                        return occurrenceDate
            )
            let occurrences = (
                for occurrence in ${ReminderOccurrence::class.collection()}
                    filter occurrence.${f(ReminderOccurrence::reminder)} == reminder._key
                        and (
                            (occurrence.${f(ReminderOccurrence::date)} <= @end and occurrence.${f(ReminderOccurrence::date)} >= @start) or
                            (occurrence.${f(ReminderOccurrence::occurrence)} <= @end and occurrence.${f(ReminderOccurrence::occurrence)} >= @start)
                        )
                    return occurrence
            )
            return {
                reminder,
                dates,
                occurrences
            }
    """.trimIndent(),
    mapOf(
        "person" to person,
        "reminders" to reminders,
        "start" to start,
        "end" to end,
    )
)

fun Db.occurrence(reminder: String, occurrence: Instant) = one(
    ReminderOccurrence::class,
    """
        for x in @@collection
            filter x.${f(ReminderOccurrence::reminder)} == @reminder
                and x.${f(ReminderOccurrence::occurrence)} == @occurrence
            return x
    """.trimIndent(),
    mapOf(
        "reminder" to reminder,
        "occurrence" to occurrence
    )
)

fun Db.deleteReminderOccurrences(reminder: String) = query(
    ReminderOccurrence::class,
    """
        for x in ${ReminderOccurrence::class.collection()}
            filter x.${f(ReminderOccurrence::reminder)} == @reminder
            remove x in ${ReminderOccurrence::class.collection()}
    """.trimIndent(),
    mapOf(
        "reminder" to reminder
    )
)

fun Db.upsertReminderOccurrenceGone(reminder: String, occurrence: Instant, gone: Boolean) = query(
    ReminderOccurrence::class,
    """
        upsert {
            ${f(ReminderOccurrence::reminder)}: @reminder,
            ${f(ReminderOccurrence::occurrence)}: @occurrence
        }
            insert {
                ${f(ReminderOccurrence::reminder)}: @reminder,
                ${f(ReminderOccurrence::occurrence)}: @occurrence,
                ${f(ReminderOccurrence::date)}: @occurrence,
                ${f(ReminderOccurrence::gone)}: @gone,
                ${f(ReminderOccurrence::createdAt)}: DATE_ISO8601(DATE_NOW())
            }
            update {
                ${f(ReminderOccurrence::gone)}: @gone
            }
            in ${ReminderOccurrence::class.collection()}
            return NEW || OLD
    """.trimIndent(),
    mapOf(
        "reminder" to reminder,
        "occurrence" to occurrence,
        "gone" to gone
    )
)
