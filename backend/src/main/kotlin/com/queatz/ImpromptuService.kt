package com.queatz

import com.queatz.db.ImpromptuHistory
import com.queatz.db.ImpromptuLocationUpdates
import com.queatz.db.Person
import com.queatz.db.allActiveImpromptu
import com.queatz.db.processAllImpromptu
import com.queatz.plugins.db
import com.queatz.plugins.notify
import com.queatz.push.ImpromptuPushData
import com.queatz.push.PushAction
import com.queatz.push.PushData
import com.queatz.push.UpdateLocationPushData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

val impromptuService = ImpromptuService()

class ImpromptuService {
    private lateinit var scope: CoroutineScope

    fun start(coroutineScope: CoroutineScope) {
        this.scope = coroutineScope

        val hour = MutableStateFlow((Clock.System.now() - 1.hours).startOfHour())

        scope.launch {
            while (true) {
                delayUntilNextHour()
                hour.update { Clock.System.now().startOfHour() }
            }
        }

        scope.launch {
            hour.collect {
                sendNotifications()
                updateLocations()
            }
        }
    }

    private fun updateLocations() {
        scope.launch {
            runCatching {
                val now = Clock.System.now()

                // Find all Impromptu settings with mode Friends or Everyone
                db.allActiveImpromptu().forEach { impromptu ->
                    // Get the person
                    val person = impromptu.personDetails ?: return@forEach

                    // Check if location update is needed based on ImpromptuLocationUpdates and Person.geoUpdatedAt
                    val shouldUpdate = when (impromptu.updateLocation) {
                        ImpromptuLocationUpdates.Hourly -> {
                            val lastUpdate = person.geoUpdatedAt
                            if (lastUpdate == null) {
                                true
                            } else {
                                val duration = now - lastUpdate
                                duration.inWholeHours >= 1
                            }
                        }

                        ImpromptuLocationUpdates.Daily -> {
                            val lastUpdate = person.geoUpdatedAt
                            if (lastUpdate == null) {
                                true
                            } else {
                                val duration = now - lastUpdate
                                duration.inWholeHours >= 24
                            }
                        }

                        ImpromptuLocationUpdates.Weekly -> {
                            val lastUpdate = person.geoUpdatedAt
                            if (lastUpdate == null) {
                                true
                            } else {
                                val duration = now - lastUpdate
                                duration.inWholeHours >= 24 * 7
                            }
                        }

                        else -> false
                    }

                    if (shouldUpdate) {
                        notify.notifyPeople(
                            people = listOf(person.id!!),
                            pushData = PushData(
                                action = PushAction.UpdateLocation,
                                data = UpdateLocationPushData(
                                    person = person.id!!
                                )
                            )
                        )
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }.getOrNull()
        }
    }

    private fun sendNotifications() {
        // Check for people near each other, match their impromptu seek settings, and send notifications
        scope.launch {
            runCatching {
                // Notifications are sent to the other person
                db.processAllImpromptu().forEach { impromptuProposal ->
                    val impromptuHistory = ImpromptuHistory(
                        person = impromptuProposal.person.id!!,
                        otherPerson = impromptuProposal.otherPerson.id!!,
                        distance = impromptuProposal.distance,
                        seeks = impromptuProposal.seeks.map { it.id!! },
                    ).let {
                        db.insert(it)
                    }

                    // Send notification to the other person
                    notify.notifyPeople(
                        people = listOf(impromptuProposal.person.id!!),
                        pushData = PushData(
                            action = PushAction.Impromptu,
                            data = ImpromptuPushData(
                                data = impromptuHistory
                            )
                        )
                    )
                }
            }
        }
    }
}
