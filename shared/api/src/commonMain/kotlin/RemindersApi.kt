package app.ailaai.api


import com.queatz.db.Reminder
import com.queatz.db.ReminderOccurrence
import com.queatz.db.ReminderOccurrences
import io.ktor.http.HttpStatusCode
import kotlin.time.Instant


suspend fun Api.reminders(
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<List<Reminder>> = {}
) = get(
    url = "reminders",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.occurrences(
    start: Instant,
    end: Instant,
    open: Boolean? = null,
    geo: List<Double>? = null,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<List<ReminderOccurrences>> = {}
) = get(
    url = "occurrences",
    onError = onError,
    onSuccess = onSuccess,
    parameters = mapOf(
        "start" to start.toString(),
        "end" to end.toString(),
        "open" to open?.toString(),
        "geo" to geo?.joinToString(",")
    ).filterValues { it != null }
)

suspend fun Api.newReminder(
    reminder: Reminder,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<Reminder> = {}
) = post(
    url = "reminders",
    body = reminder,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateReminder(
    id: String,
    reminder: Reminder,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<Reminder> = {}
) = post(
    url = "reminders/$id",
    body = reminder,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.joinReminder(
    id: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post(
    url = "reminders/$id/join",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.leaveReminder(
    id: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post(
    url = "reminders/$id/leave",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.reminder(
    id: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<Reminder> = {}
) = get(
    url = "reminders/$id",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.reminderOccurrences(
    id: String,
    start: Instant,
    end: Instant?,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<List<ReminderOccurrences>>
) = get(
    url = "reminders/$id/occurrences",
    onError = onError,
    onSuccess = onSuccess,
    parameters = mapOf(
        "start" to start.toString(),
        "end" to end?.toString()
    )
)

suspend fun Api.updateReminderOccurrence(
    id: String,
    occurrence: Instant,
    update: ReminderOccurrence,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<ReminderOccurrence> = {}
) = post(
    url = "reminders/$id/occurrences/$occurrence",
    body = update,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteReminderOccurrence(
    id: String,
    occurrence: Instant,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post(
    url = "reminders/$id/occurrences/$occurrence/delete",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteReminder(
    reminderId: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post(
    url = "reminders/$reminderId/delete",
    onError = onError,
    onSuccess = onSuccess
)
