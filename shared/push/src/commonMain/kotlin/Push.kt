package com.queatz.push

import com.queatz.db.*
import kotlinx.serialization.Serializable

enum class PushAction {
    Message,
    Collaboration,
    JoinRequest,
    Group,
    Call
}

enum class GroupEvent {
    Join,
    Leave
}

enum class CollaborationEvent {
    AddedPerson,
    RemovedPerson,
    AddedCard,
    RemovedCard,
    UpdatedCard,
}

enum class CollaborationEventDataDetails {
    Photo,
    Video,
    Conversation,
    Name,
    Location,
    Content,
}

enum class JoinRequestEvent {
    Request
}

@Serializable
data class PushData(
    val action: PushAction? = null,
    val data: PushDataData? = null,
)

@Serializable
sealed class PushDataData

@Serializable
data class MessagePushData(
    val group: Group,
    val person: Person,
    val message: Message,
    val show: Boolean? = null
) : PushDataData()

@Serializable
data class CallPushData(
    val group: Group,
    val person: Person,
    val show: Boolean? = null
) : PushDataData()

@Serializable
data class CollaborationPushData(
    val person: Person,
    val card: Card,
    val event: CollaborationEvent,
    val data: CollaborationEventData,
) : PushDataData()

@Serializable
data class GroupPushData(
    val person: Person,
    val group: Group,
    val event: GroupEvent,
    val details: GroupEventData? = null
) : PushDataData()

@Serializable
data class JoinRequestPushData(
    val person: Person,
    val group: Group,
    val joinRequest: JoinRequest,
    val event: JoinRequestEvent,
) : PushDataData()

@Serializable
data class CollaborationEventData (
    val card: Card? = null,
    val person: Person? = null,
    val details: CollaborationEventDataDetails? = null
)

@Serializable
data class GroupEventData (
    val invitor: Person? = null
)
