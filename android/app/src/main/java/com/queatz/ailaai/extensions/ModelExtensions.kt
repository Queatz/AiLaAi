package com.queatz.ailaai.extensions

import android.content.Context
import com.ibm.icu.text.DecimalFormat
import com.queatz.ailaai.R
import com.queatz.ailaai.data.getAttachment
import com.queatz.db.AudioAttachment
import com.queatz.db.CardAttachment
import com.queatz.db.GroupAttachment
import com.queatz.db.GroupExtended
import com.queatz.db.InventoryItem
import com.queatz.db.InventoryItemExtended
import com.queatz.db.Member
import com.queatz.db.Message
import com.queatz.db.Person
import com.queatz.db.PhotosAttachment
import com.queatz.db.StickerAttachment
import com.queatz.db.StoryAttachment
import com.queatz.db.Trade
import com.queatz.db.TradeAttachment
import com.queatz.db.TradeExtended
import com.queatz.db.UrlAttachment
import com.queatz.db.VideosAttachment
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.text.ParseException

data class ContactPhoto(
    val name: String = "",
    val photo: String? = null,
    val seen: Instant? = null
)

fun Person.contactPhoto() = ContactPhoto(
    name ?: "",
    photo,
    seen
)

fun GroupExtended.isGroupLike(omitGroupsWith: Person? = null) = group?.name?.isNotBlank() == true && members?.none { it.person?.id == omitGroupsWith?.id } == true

// todo make this @Composable and autofull strings
fun GroupExtended.name(someone: String, emptyGroup: String, omit: List<String>) =
    group?.name?.nullIfBlank
        ?: members
            ?.filter { !omit.contains(it.person?.id) }
            ?.mapNotNull { it.person }
            ?.joinToString { it.name ?: someone }
            ?.nullIfBlank
        ?: emptyGroup

fun GroupExtended.photos(omit: List<Person> = emptyList(), ifEmpty: List<Person>? = null) = members
    ?.filter {
        omit.none { person -> it.person?.id == person.id }
    }
    ?.map {
        ContactPhoto(it.person?.name ?: "", it.person?.photo, it.person?.seen)
    }?.takeIf { it.isNotEmpty() }
    ?: ifEmpty?.map {
        ContactPhoto(it.name ?: "", it.photo, it.seen)
    }
    ?: listOf(ContactPhoto())

fun GroupExtended.isUnread(member: Member?): Boolean {
    return (member?.seen?.toEpochMilliseconds() ?: return false) < (latestMessage?.createdAt?.toEpochMilliseconds() ?: return false)
}

fun Message.attachmentText(context: Context): String? = when (val attachment = getAttachment()) {
    is CardAttachment -> {
        if (attachment.card != null) {
            context.getString(R.string.sent_a_card)
        } else {
            null
        }
    }
    is PhotosAttachment -> {
        context.resources.getQuantityString(R.plurals.sent_photos, attachment.photos?.size ?: 0, attachment.photos?.size ?: 0)
    }
    is AudioAttachment -> {
        context.resources.getString(R.string.sent_audio)
    }
    is VideosAttachment -> {
        context.resources.getQuantityString(R.plurals.sent_videos, attachment.videos?.size ?: 0, attachment.videos?.size ?: 0)
    }
    is StoryAttachment -> {
        context.resources.getString(R.string.sent_a_story)
    }
    is GroupAttachment -> {
        context.resources.getString(R.string.sent_a_group)
    }
    is StickerAttachment -> {
        context.resources.getString(R.string.sent_a_sticker)
    }
    is UrlAttachment -> {
        context.resources.getString(R.string.sent_a_link)
    }
    is TradeAttachment -> {
        context.resources.getString(R.string.sent_a_trade)
    }
    else -> null
}

val InventoryItem.isExpired get() = expiresAt?.let { it < Clock.System.now() } ?: false

private val itemFormat = DecimalFormat("#.######")

fun Number.formatItemQuantity() = itemFormat.format(this)!!
fun String.toItemQuantity() = try {
    itemFormat.parse(this).toDouble()
} catch (_: ParseException) {
    null
}

data class TradeItemQuantity(
    val inventoryItem: InventoryItemExtended,
    val quantity: Double
)

val TradeExtended.items: List<TradeItemQuantity> get() = trade!!.members!!.flatMap { it.items ?: emptyList() }.mapNotNull { tradeItem ->
    val inventoryItem = inventoryItems?.firstOrNull {
        it.inventoryItem!!.id == tradeItem.inventoryItem
    } ?: return@mapNotNull null

    TradeItemQuantity(inventoryItem, tradeItem.quantity!!)
}
