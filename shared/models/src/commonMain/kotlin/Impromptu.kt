package com.queatz.db

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Impromptu(
    var person: String? = null,
    var mode: ImpromptuMode? = null,
    var updateLocation: ImpromptuLocationUpdates? = null,
    var notificationType: ImpromptuNotificationStyle? = null,

    // Transient from db
    var seek: List<ImpromptuSeek>? = null,
    // Transient from db
    var offer: List<ImpromptuSeek>? = null,
    // Transient from db
    var personDetails: Person? = null,
) : Model()

@Serializable
data class ImpromptuSeek(
    var person: String? = null,
    var name: String? = null,
    var offer: Boolean? = null,
    var radius: Double? = null, // in kilometers
    var expiresAt: Instant? = null
) : Model()

@Serializable
data class ImpromptuHistory(
    // The person who got this notification
    var person: String? = null,
    // If the history was deleted
    var gone: Boolean? = null,
    // The person this notification is about
    var otherPerson: String? = null,
    // The distance in meters between the two people
    var distance: Double? = null,
    // The ImpromptuSeeks that triggered this notification
    val seeks: List<String> = emptyList(),

    // Transient from db
    var otherPersonDetails: Person? = null,

    // Transient from db
    var seeksDetails: List<ImpromptuSeek>? = null,
) : Model()

enum class ImpromptuMode {
    Off,
    Friends,
    Everyone
}

enum class ImpromptuLocationUpdates {
    Off,
    Hourly,
    Daily,
    Weekly
}

enum class ImpromptuNotificationStyle {
    Normal,
    Passive
}
