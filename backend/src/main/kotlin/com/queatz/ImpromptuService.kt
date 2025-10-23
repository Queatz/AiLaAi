package com.queatz

import com.queatz.db.ImpromptuHistory
import com.queatz.db.ImpromptuLocationUpdates
import com.queatz.db.ImpromptuNotificationStyle
import com.queatz.db.allActiveImpromptu
import com.queatz.db.friends
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
import kotlin.time.Clock
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.logging.Logger
import kotlin.time.Duration.Companion.hours

val impromptuService = ImpromptuService()

class ImpromptuService {
    private lateinit var scope: CoroutineScope

    fun start(coroutineScope: CoroutineScope) {
        this.scope = coroutineScope

        val hour = MutableStateFlow((Clock.System.now() - 1.hours).startOfHour())

        scope.launch {
            while (true) {
                delayUntilNextMinute()
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
                db.allActiveImpromptu().also {
                    Logger.getAnonymousLogger().info("IMPROMPTU updateLocations found ${it.size} active impromptus for location updates")
                }.forEach { impromptu ->
                    // Get the person
                    val person = impromptu.personDetails ?: return@forEach

                    // Check if location update is needed based on ImpromptuLocationUpdates and Person.geoUpdatedAt
                    val shouldUpdate = when (impromptu.updateLocation) {
                        ImpromptuLocationUpdates.Hourly -> {

                            val utcOffset = person.utcOffset?.let {
                                val hours = it.hours.inWholeHours.toInt()
                                UtcOffset(
                                    hours = hours,
                                    minutes = (it - hours).hours.inWholeMinutes.toInt()
                                )
                            } ?: UtcOffset.ZERO

                            // Calculate local hour for the person
                            val localHour = now.toLocalDateTime(utcOffset.asTimeZone()).hour

                            // Don't update if it's before 7am or after 7pm local time
                            if (localHour < 7 || localHour >= 19) {
                                false
                            } else {
                                val lastUpdate = person.geoUpdatedAt
                                if (lastUpdate == null) {
                                    true
                                } else {
                                    val duration = now - lastUpdate
                                    duration.inWholeHours >= 1
                                }
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
                        Logger.getAnonymousLogger().info("IMPROMPTU updateLocations will update location for ${person.id!!}")

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
            }
        }
    }

    private fun sendNotifications() {
        Logger.getAnonymousLogger().info("IMPROMPTU sendNotifications started")
        // Check for people near each other, match their impromptu seek settings, and send notifications
        scope.launch {
            runCatching {
                // Notifications are sent to the other person
                db.processAllImpromptu().also {
                    Logger.getAnonymousLogger().info("IMPROMPTU processAllImpromptu found ${it.size} impromptus to process")
                }.forEach { impromptuProposal ->
                    val impromptuHistory = ImpromptuHistory(
                        person = impromptuProposal.person.id!!,
                        otherPerson = impromptuProposal.otherPerson.id!!,
                        distance = impromptuProposal.distance,
                        seeks = impromptuProposal.seeks.map { it.id!! },
                    ).let {
                        db.insert(it)
                    }.also {
                        // Needed for the push notification
                        it.otherPersonDetails = impromptuProposal.otherPerson
                        // Needed for the push notification
                        it.seeksDetails = impromptuProposal.seeks
                    }

                    if (!impromptuProposal.everyone) {
                        if (
                            !db.friends(
                                listOf(
                                    impromptuProposal.person.id!!,
                                    impromptuProposal.otherPerson.id!!
                                )
                            )
                        ) {
                            return@forEach
                        }
                    }

                    // Send notification to the other person
                    notify.notifyPeople(
                        people = listOf(impromptuProposal.person.id!!),
                        pushData = PushData(
                            action = PushAction.Impromptu,
                            data = ImpromptuPushData(
                                notificationType = impromptuProposal.notificationType
                                    ?: ImpromptuNotificationStyle.Normal,
                                data = impromptuHistory
                            )
                        )
                    )
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}
