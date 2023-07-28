package com.queatz.ailaai.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
class Card(
    var person: String? = null,
    var parent: String? = null,
    var name: String? = null,
    var photo: String? = null,
    var video: String? = null,
    var location: String? = null,
    var collaborators: List<String>? = null,
    var categories: List<String>? = null,
    var equipped: Boolean? = null,
    var offline: Boolean? = null,
    var geo: List<Double>? = null,
    var conversation: String? = null,
    var active: Boolean? = null,
    var cardCount: Int? = null
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
class Group(
    var name: String? = null,
    var seen: Instant? = null,
    var description: String? = null,
) : Model()

@Serializable
class Crash(
    var details: String? = null,
) : Model()

@Serializable
class Person(
    var name: String? = null,
    var photo: String? = null,
    var inviter: String? = null,
    var seen: Instant? = null,
    var geo: List<Double>? = null,
    var language: String? = null,
    var source: PersonSource? = null
) : Model()

@Serializable
class Profile(
    var person: String? = null,
    var photo: String? = null,
    var video: String? = null,
    var about: String? = null,
    var url: String? = null
) : Model()

@Serializable
class Member(
    var seen: Instant? = null,
    var hide: Boolean? = null,
    var gone: Boolean? = null,
    var host: Boolean? = null
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
    var message: String? = null,
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
class Presence(
    var person: String? = null,
    var readStoriesUntil: Instant? = null,
    var unreadStoriesCount: Int? = null
) : Model()

@Serializable
class Transfer(
    var code: String? = null,
    var person: String? = null
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
class AppFeedback(
    var feedback: String? = null,
    var person: String? = null,
    var type: AppFeedbackType? = null
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
open class Edge : Model() {
    var from: String? = null
    var to: String? = null
}

@Serializable
open class Model {
    var id: String? = null
    var createdAt: Instant? = null
}

enum class PersonSource {
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

