package com.queatz.ailaai.api

import com.queatz.ailaai.data.*
import io.ktor.http.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
class ReminderOccurrences(
    val reminder: Reminder,
    val dates: List<Instant>,
    val occurrences: List<ReminderOccurrence>,
)

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
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<List<ReminderOccurrences>> = {}
) = get(
    url = "occurrences",
    onError = onError,
    onSuccess = onSuccess,
    parameters = mapOf(
        "start" to start.toString(),
        "end" to end.toString()
    )
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
