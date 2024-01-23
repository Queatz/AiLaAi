package com.queatz

import ReminderEvent
import com.queatz.db.occurrences
import com.queatz.plugins.db
import com.queatz.plugins.notify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import toEvents
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val remind = Remind()

class Remind {
    lateinit var scope: CoroutineScope

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

    private fun pushReminders(events: List<ReminderEvent>) {
        Logger.getAnonymousLogger().info("REMIND events=${events.size}")
        events.forEach {
            Logger.getAnonymousLogger().info("REMIND reminder=${it.reminder.title} date=${it.date} event=${it.event}")
            notify.reminder(it.reminder, it.occurrence)
        }
    }
}
