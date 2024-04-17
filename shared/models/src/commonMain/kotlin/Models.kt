package com.queatz.db

import com.queatz.widgets.Widgets
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
class Person(
    var name: String? = null,
    var photo: String? = null,
    var geo: List<Double>? = null,
    var inviter: String? = null,
    var seen: Instant? = null,
    var language: String? = null,
    var source: PersonSource? = null
) : Model()

@Serializable
class Settings(
    var person: String? = null,
    var language: String? = null
) : Model()

@Serializable
class Report(
    var reporter: String? = null,
    var reporterMessage: String? = null,
    var urgent: Boolean? = null,
    var entity: String? = null,
    var type: ReportType? = null
) : Model()

@Serializable
class Presence(
    var person: String? = null,
    var readStoriesUntil: Instant? = null,
    var unreadStoriesCount: Int? = null
) : Model()

@Serializable
class Profile(
    var person: String? = null,
    var photo: String? = null,
    var background: String? = null,
    var video: String? = null,
    var about: String? = null,
    var content: String? = null,
    var url: String? = null
) : Model()

@Serializable
class Invite(
    var person: String? = null,
    var group: String? = null,
    var about: String? = null,
    var code: String? = null,
    var expiry: Instant? = null,
    var remaining: Int? = null,
    var total: Int? = null
) : Model()

@Serializable
class Save(
    var person: String? = null,
    var card: String? = null
) : Model()

@Serializable
class Transfer(
    var person: String? = null,
    var code: String? = null
) : Model()

@Serializable
class LinkDeviceToken(
    var token: String? = null,
    var person: String? = null
) : Model()

@Serializable
class Crash(
    var details: String? = null
) : Model()

@Serializable
class AppFeedback(
    var feedback: String? = null,
    var person: String? = null,
    var type: AppFeedbackType? = null
) : Model()

@Serializable
class Card(
    var person: String? = null,
    var parent: String? = null,
    var group: String? = null,
    var name: String? = null,
    var photo: String? = null,
    var video: String? = null,
    var background: String? = null,
    var location: String? = null,
    var collaborators: List<String>? = null,
    var categories: List<String>? = null,
    var equipped: Boolean? = null,
    var offline: Boolean? = null,
    var geo: List<Double>? = null,
    var conversation: String? = null,
    var content: String? = null,
    var options: String? = null,
    var pay: Pay? = null,
    var active: Boolean? = null,
    var cardCount: Int? = null
) : Model()

@Serializable
class Reaction(
    var reaction: String? = null,
    var comment: String? = null
) : Edge()

@Serializable
class Group(
    var name: String? = null,
    var photo: String? = null,
    var video: String? = null,
    var background: String? = null,
    var seen: Instant? = null,
    var description: String? = null,
    var categories: List<String>? = null,
    var geo: List<Double>? = null,
    var open: Boolean? = null,
    var config: GroupConfig? = null
) : Model()

@Serializable
data class GroupConfig(
    var messages: GroupMessagesConfig? = null,
    var edits: GroupEditsConfig? = null
)

enum class GroupMessagesConfig {
    Hosts
}

enum class GroupEditsConfig {
    Hosts
}

@Serializable
class Call(
    var group: String? = null,
    var room: String? = null,
    var participants: Int? = null,
) : Model()

enum class PayFrequency {
    Hourly,
    Daily,
    Weekly,
    Monthly,
    Yearly
}

@Serializable
data class Pay(
    var pay: String? = null,
    var frequency: PayFrequency? = null
)

@Serializable
data class Trade(
    var people: List<String>? = null,
    var members: List<TradeMember>? = null,
    var note: String? = null,
    var initiator: String? = null,
    var completedAt: Instant? = null,
    var cancelledAt: Instant? = null
) : Model()

@Serializable
data class TradeMember(
    var person: String? = null,
    var confirmed: Boolean? = null,
    var items: List<TradeItem>? = null
)

@Serializable
data class TradeItem(
    var inventoryItem: String? = null,
    var quantity: Double? = null,
    var to: String? = null
)

@Serializable
data class Item(
    var name: String? = null,
    var photo: String? = null,
    var description: String? = null,
    var creator: String? = null,
    var divisible: Boolean? = null,
    var lifespan: Int? = null,
    var becomes: String? = null
) : Model()

@Serializable
data class Inventory(
    var person: String? = null,
    var card: String? = null,
    var group: String? = null,
    var geo: List<Double>? = null
) : Model()

@Serializable
data class InventoryItem(
    var item: String? = null,
    var inventory: String? = null,
    var quantity: Double? = null,
    var equipped: Boolean? = null,
    var expiresAt: Instant? = null
) : Model()

@Serializable
class JoinRequest(
    var person: String? = null,
    var group: String? = null,
    var message: String? = null
) : Model()

@Serializable
class Member(
    var seen: Instant? = null,
    var hide: Boolean? = null,
    var gone: Boolean? = null,
    var host: Boolean? = null,
    var snoozed: Boolean? = null,
    var snoozedUntil: Instant? = null,
) : Edge()

@Serializable
class Subscription(
) : Edge()

@Serializable
class Message(
    var group: String? = null,
    var member: String? = null,
    var text: String? = null,
    var attachment: String? = null,
    var attachments: List<String>? = null
) : Model()

@Serializable
class Sticker(
    var photo: String? = null,
    var pack: String? = null,
    var name: String? = null,
    var message: String? = null
) : Model()

@Serializable
class StickerPack(
    var name: String? = null,
    var description: String? = null,
    var person: String? = null,
    var active: Boolean? = null,
    var stickers: List<Sticker>? = null
) : Model()

@Serializable
class StickerPackSave : Edge()

@Serializable
class Device(
    var person: String? = null,
    var type: DeviceType? = null,
    var token: String? = null
) : Model()

@Serializable
class Search(
    var search: String? = null,
    var source: SearchSource? = null
) : Model()

@Serializable
class Story(
    var person: String? = null,
    var title: String? = null,
    var url: String? = null,
    var geo: List<Double>? = null,
    var publishDate: Instant? = null,
    var published: Boolean? = null,
    var content: String? = null,
    var authors: List<Person>? = null
) : Model()

@Serializable
class StoryDraft(
    var story: String? = null,
    var groups: List<String>? = null,
    var groupDetails: List<Group>? = null
) : Model()

@Serializable
class Reminder(
    var person: String? = null,
    var groups: List<String>? = null,
    var attachment: String? = null,
    var title: String? = null,
    var note: String? = null,
    var start: Instant? = null,
    var end: Instant? = null,
    var timezone: String? = null,
    var utcOffset: Double? = null,
    var schedule: ReminderSchedule? = null
) : Model()

@Serializable
class ReminderOccurrence(
    var reminder: String? = null,
    var occurrence: Instant? = null,
    var date: Instant? = null,
    var note: String? = null,
    var done: Boolean? = null,
    var gone: Boolean? = null,
) : Model()

@Serializable
class Widget(
    var person: String? = null,
    var widget: Widgets? = null,
    var data: String? = null
) : Model()

@Serializable
class Script(
    var person: String? = null,
    var name: String? = null,
    var description: String? = null,
    var source: String? = null
) : Model()

@Serializable
data class ReminderSchedule(
    /**
     * 0 - 23 = hour of day
     *
     * Described in reminder's local time.
     */
    val hours: List<Int>? = null,
    /**
     * 1 - 31 = day of month
     * -1 - -31 = last days of month
     *
     * Described in reminder's local time.
     */
    val days: List<Int>? = null,
    /**
     * 1 - 7 = day of week
     *
     * Described in reminder's local time.
     */
    val weekdays: List<Int>? = null,
    /**
     * 1 - 5 = week of month
     *
     * Described in reminder's local time.
     */
    val weeks: List<Int>? = null,
    /**
     * 1 - 12 = month of year
     *
     * Described in reminder's local time.
     */
    val months: List<Int>? = null,
    /**
     * year
     *
     * Described in reminder's local time.
     */
    val years: List<Int>? = null,
)

enum class DeviceType {
    Hms,
    Gms,
    Web,
    Apns
}

enum class PersonSource {
    Web
}

enum class SearchSource {
    Web
}

enum class AppFeedbackType {
    Suggestion,
    Issue,
    Other
}

enum class ReportType {
    Safety,
    Content,
    Spam,
    Other
}
@Serializable
expect open class Model() {
    var id: String?
    var createdAt: Instant?
}

@Serializable
expect open class Edge() : Model {
    var from: String?
    var to: String?
}
