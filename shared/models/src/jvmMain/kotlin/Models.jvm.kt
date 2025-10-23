package com.queatz.db

import com.arangodb.serde.jackson.From
import com.arangodb.serde.jackson.Key
import com.arangodb.serde.jackson.To
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.time.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
actual open class Model {

    @Key
    @JsonNames("id", "_key")
    @JsonProperty("_key")
    @JsonAlias("id")
    actual var id: String? = null

    actual var createdAt: Instant? = null
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
actual open class Edge : Model() {
    @From
    @JsonNames("from", "_from")
    @JsonProperty("_from")
    @JsonAlias("from")
    actual var from: String? = null

    @To
    @JsonNames("to", "_to")
    @JsonProperty("_to")
    @JsonAlias("to")
    actual var to: String? = null
}
