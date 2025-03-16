package com.queatz.db

import com.queatz.widgets.Widgets
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Geo(val latitude: Double, val longitude: Double) {
    override fun toString() = "$latitude,$longitude"
}

fun Geo.toList() = listOf(latitude, longitude)
fun List<Double>.asGeo() = Geo(get(0), get(1))

@Serializable
class MemberDevice(
    var member: Member? = null,
    var devices: List<Device>? = null
)

@Serializable
class GroupExtended(
    var group: Group? = null,
    var members: List<MemberAndPerson>? = null,
    var bots: List<Bot>? = null,
    var cardCount: Int? = null,
    var botCount: Int? = null,
    var latestMessage: Message? = null,
    var pin: Boolean? = null
)

@Serializable
class MemberAndPerson(
    var person: Person? = null,
    var member: Member? = null
)

@Serializable
class JoinRequestAndPerson(
    var person: Person? = null,
    var joinRequest: JoinRequest? = null
)

@Serializable
class SaveAndCard(
    var save: Save? = null,
    var card: Card? = null
)

@Serializable
class ReminderOccurrences(
    val reminder: Reminder,
    val dates: List<Instant>,
    val occurrences: List<ReminderOccurrence>,
)

@Serializable
data class WildReplyBody(
    val message: String,
    val conversation: String?,
    val card: String,
    val device: String
)

@Serializable
data class ConversationItem(
    var title: String = "",
    var message: String = "",
    var action: ConversationAction? = null,
    var items: MutableList<ConversationItem> = mutableListOf(),
)

@Serializable
data class CardOptions(
    var enableReplies: Boolean? = null,
    var enableAnonymousReplies: Boolean? = null
)

enum class ConversationAction {
    Message
}

@Serializable
data class ProfileStats(
    val friendsCount: Int = 0,
    val cardCount: Int = 0,
    val storiesCount: Int = 0,
    val subscriberCount: Int = 0,
)

@Serializable
data class PersonProfile(
    val person: Person,
    val profile: Profile,
    val stats: ProfileStats,
    val subscription: Subscription?
)

@Serializable
data class ExportDataResponse(
    val profile: Profile? = null,
    val cards: List<Card>? = null,
    val stories: List<Story>? = null
)

@Serializable
class MyDevice(
    val type: DeviceType,
    val token: String,
)

@Serializable
class AiPhotoRequest(
    val prompt: String,
    val style: String? = null,
    val aspect: Double? = null,
    val removeBackground: Boolean? = null,
)

@Serializable
class AiSpeakRequest(
    val text: String
)

@Serializable
class AiPhotoResponse(
    val photo: String
)

@Serializable
data class VersionInfo(
    val versionCode: Int,
    val versionName: String
)

@Serializable
data class SignUpRequest(
    val code: String?
)

@Serializable
data class SignInRequest(
    val code: String? = null,
    val link: String? = null
)

@Serializable
data class TokenResponse (
    val token: String
)

@Serializable
data class UploadResponse (
    val urls: List<String>
)

@Serializable
data class CallAndToken (
    val call: Call,
    val token: String
)

@Serializable
data class CreateGroupBody(val people: List<String>, val reuse: Boolean)

@Serializable
data class SearchGroupBody(val people: List<String>)

@Serializable
data class CreateWidgetBody(val widget: Widgets, val data: String? = null)

@Serializable
data class RunWidgetBody(val data: String? = null)

@Serializable
data class RunWidgetResponse(
    val content: List<StoryContent>? = null
)

@Serializable
data class MintItemBody(
    val quantity: Double
)

@Serializable
data class DropItemBody(
    val quantity: Double,
    val geo: List<Double>? = null,
)

@Serializable
data class EquipItemBody(
    val quantity: Double
)

@Serializable
data class UnequipItemBody(
    val quantity: Double
)

@Serializable
data class TakeInventoryBody(
    val items: List<TakeInventoryItem>
)

@Serializable
data class TakeInventoryItem(
    val inventoryItem: String,
    val quantity: Double
)

@Serializable
data class InventoryItemExtended(
    val item: Item? = null,
    val inventoryItem: InventoryItem? = null
)

@Serializable
data class ItemExtended(
    val item: Item? = null,
    val inventory: Double? = null,
    val circulating: Double? = null,
    val expired: Double? = null
)

@Serializable
data class TradeExtended(
    val trade: Trade? = null,
    val people: List<Person>? = null,
    val inventoryItems: List<InventoryItemExtended>? = null
)

@Serializable
data class RunScriptBody(
    val data: String? = null
)

@Serializable
data class ScriptResult(
    val content: List<StoryContent>? = null
)

@Serializable
data class ReactBody(
    val reaction: Reaction,
    val remove: Boolean? = null
)

@Serializable
data class ReactionAndPerson(
    val reaction: Reaction? = null,
    val person: Person? = null
)

@Serializable
data class CommentExtended(
    val comment: Comment? = null,
    val person: Person? = null,
    val replies: List<CommentExtended>? = null,
    val totalReplies: Int? = null
)

@Serializable
data class PlatformMeResponse(
    val host: Boolean
)

@Serializable
data class AppStats(
    val activePeople30Days: Int,
    val activePeople7Days: Int,
    val activePeople24Hours: Int,
    val newPeople30Days: Int,
    val newPeople7Days: Int,
    val newPeople24Hours: Int,
    val totalPeople: Int,
    val totalDraftCards: Int,
    val totalPublishedCards: Int,
    val totalDraftStories: Int,
    val totalPublishedStories: Int,
    val totalClosedGroups: Int,
    val totalOpenGroups: Int,
    val totalReminders: Int,
    val totalItems: Int
)

@Serializable
data class StatsHealth(
    val diskUsagePercent: Double
)

@Serializable
data class BotDetailsBody(
    val url: String,
    val photo: String? = null,
    val data: BotData? = null
)

@Serializable
data class AppDetailsBody(
    val url: String,
    val photo: String? = null,
    val data: AppData? = null
)

@Serializable
class GroupBotExtended(
    var bot: Bot? = null,
    var groupBot: GroupBot? = null
)

@Serializable
class CardUpgradeBody(
    var level: Int
)

@Serializable
class CardDowngradeBody(
    var level: Int
)

@Serializable
class CardUpgradeDetails(
    var level: Int,
    var points: Int,
    var available: Boolean
)

@Serializable
class CardDowngradeDetails(
    var level: Int,
    var points: Int
)

@Serializable
class PlatformAccountsPointsBody(
    var add: Int
)
