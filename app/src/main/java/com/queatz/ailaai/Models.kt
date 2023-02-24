package com.queatz.ailaai

import kotlinx.datetime.Instant

class Card(
    var person: String? = null,
    var parent: String? = null,
    var name: String? = null,
    var photo: String? = null,
    var location: String? = null,
    var equipped: Boolean? = null,
    var geo: List<Double>? = null,
    var conversation: String? = null,
    var active: Boolean? = null,
    var cardCount: Int? = null
) : Model()

class Invite(
    var person: String? = null,
    var code: String? = null
) : Model()

class Group(
    var name: String? = null,
    var seen: Instant? = null
) : Model()

class Person(
    var name: String? = null,
    var photo: String? = null,
    var seen: Instant? = null,
    var inviter: String? = null
) : Model()

class Member(
    var seen: Instant? = null,
    var hide: Boolean? = null,
    var gone: Boolean? = null
) : Edge()

class Message(
    var group: String? = null,
    var member: String? = null,
    var text: String? = null,
    var attachment: String? = null
) : Model()

class Transfer(
    var code: String? = null
) : Model()

open class Edge : Model() {
    var from: String? = null
    var to: String? = null
}

open class Model {
    var id: String? = null
    var createdAt: Instant? = null
}
