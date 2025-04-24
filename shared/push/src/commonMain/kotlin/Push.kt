package com.queatz.push

import com.queatz.db.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class PushAction {
    Message,
    Collaboration,
    JoinRequest,
    Group,
    Call,
    CallStatus,
    Reminder,
    Trade,
    Story,
    Comment,
    CommentReply,
    MessageReaction,
    UpdateLocation,
    Impromptu
}

enum class StoryEvent {
    Posted
}

enum class GroupEvent {
    Join,
    Leave
}

enum class TradeEvent {
    Started,
    Updated,
    Completed,
    Cancelled
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
    val person: Person? = null,
    val bot: Bot? = null,
    val message: Message,
    val show: Boolean? = null
) : PushDataData()

@Serializable
data class MessageReactionPushData(
    val group: Group,
    val person: Person? = null,
    val message: Message? = null,
    val react: ReactBody? = null,
    val show: Boolean? = null
) : PushDataData()

@Serializable
data class CallPushData(
    val group: Group,
    val person: Person,
    val show: Boolean? = null
) : PushDataData()

@Serializable
data class CallStatusPushData(
    val call: Call
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
data class ReminderPushData(
    val date: Instant,
    val reminder: Reminder,
    val occurrence: ReminderOccurrence?,
    val show: Boolean? = null
) : PushDataData()

@Serializable
data class TradePushData(
    val trade: Trade,
    val people: List<Person>? = null,
    val person: Person,
    val event: TradeEvent
) : PushDataData()

@Serializable
data class StoryPushData(
    val authors: List<Person>,
    val story: Story,
    val event: StoryEvent
) : PushDataData()

@Serializable
data class CollaborationEventData(
    val card: Card? = null,
    val person: Person? = null,
    val details: CollaborationEventDataDetails? = null
)

@Serializable
data class GroupEventData(
    val invitor: Person? = null
)

@Serializable
data class CommentPushData(
    val comment: Comment? = null,
    val story: Story? = null,
    val person: Person? = null
) : PushDataData()

@Serializable
data class CommentReplyPushData(
    val comment: Comment? = null,
    val onComment: Comment? = null,
    val story: Story? = null,
    val person: Person? = null
) : PushDataData()

@Serializable
data class UpdateLocationPushData(
    val person: String? = null
) : PushDataData()

@Serializable
data class ImpromptuPushData(
    val data: ImpromptuHistory? = null
) : PushDataData()
