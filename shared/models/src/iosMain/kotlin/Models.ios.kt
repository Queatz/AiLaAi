package com.queatz.db

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
actual open class Model {
    actual var id: String? = null
    actual var createdAt: Instant? = null
}

@Serializable
actual open class Edge : Model() {
    actual var from: String? = null
    actual var to: String? = null
}
