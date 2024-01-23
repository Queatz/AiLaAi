package com.queatz

import com.queatz.db.ReminderOccurrences
import com.queatz.db.occurrences
import com.queatz.plugins.db
import com.queatz.plugins.notify
import com.queatz.plugins.push
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val remind = Remind()

class Remind {
    lateinit var scope: CoroutineScope

    fun start(scope: CoroutineScope) {
        this.scope = scope

        var minute = MutableStateFlow((Clock.System.now() - 1.minutes).startOfMinute())

        scope.launch {
            while (true) {
                delayUntilNextMinute()
                minute.update { Clock.System.now().startOfMinute() }
            }
        }

        scope.launch {
            minute.collect {
                pushReminders(
                    db.occurrences(null, it, it + 59.seconds + 999.milliseconds)
                )
            }
        }
    }

    private fun pushReminders(occurrences: List<ReminderOccurrences>) {
        occurrences.forEach {
            it.dates.forEach { date ->
                val occurrence = it.occurrences.firstOrNull { it.date == date }
                notify.reminder(it.reminder, occurrence)
            }
        }
    }
}
