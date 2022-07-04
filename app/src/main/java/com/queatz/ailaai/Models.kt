package com.queatz.ailaai

import kotlinx.datetime.Instant

class Card(
    var person: String? = null,
    var name: String? = null,
    var photo: String? = null,
    var location: String? = null,
    var geo: List<Double>? = null,
    var conversation: String? = null,
    var active: Boolean? = null
) : Model()

class Invite(
    var person: String? = null,
    var code: String? = null
) : Model()

open class Model {
    var id: String? = null

    var createdAt: Instant? = null
}
