package com.queatz.ailaai.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.*
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.attachmentText
import com.queatz.ailaai.extensions.nullIfBlank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

val push by lazy {
    Push()
}

class Push {

    private lateinit var context: Context
    var navController: NavController? = null
    var latestEvent: Lifecycle.Event? = null
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val meKey = stringPreferencesKey("me")
    private var meId: String? = null

    private val latestMessageFlow = MutableSharedFlow<String?>()
    val latestMessage: Flow<String?> = latestMessageFlow

    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    suspend fun setMe(id: String) {
        meId = id
        context.dataStore.edit {
            it[meKey] = id
        }
    }

    fun receive(data: Map<String, String>) {
        if (!data.containsKey("action")) {
            Log.d("PUSH", "Push notification does not contain 'action'")
            return
        }

        Log.d("PUSH", "Got push: ${data["action"]}")

        val action = data["action"]!!

        try {
            when (PushAction.valueOf(action)) {
                PushAction.Message -> receive(parse<MessagePushData>(data["data"]!!))
                PushAction.Collaboration -> receive(parse<CollaborationPushData>(data["data"]!!))
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private inline fun <reified T : Any> parse(action: String): T = json.decodeFromString<T>(action)

    private fun receive(data: CollaborationPushData) {
        val deeplinkIntent = Intent(
            Intent.ACTION_VIEW,
            "$appDomain/card/${data.card.id}".toUri(),
            context,
            MainActivity::class.java
        )

        send(
            deeplinkIntent,
            Notifications.Collaboration,
            groupKey = "collaboration/${data.card.id}",
            title = data.card.name ?: context.getString(R.string.collaboration),
            text = eventForCollaborationNotification(data)
        )
    }

    private fun eventForCollaborationNotification(data: CollaborationPushData): String {
        val person = data.person.name ?: context.getString(R.string.someone)
        return when (data.event) {
            CollaborationEvent.AddedPerson -> context.getString(R.string.person_added_person, person, personNameOrYou(data.data.person))
            CollaborationEvent.RemovedPerson -> context.getString(R.string.person_removed_person, person, personNameOrYou(data.data.person))
            CollaborationEvent.AddedCard -> context.getString(
                R.string.person_added_card, person, data.data.card?.name ?: context.getString(
                    R.string.inline_a_card
                ))
            CollaborationEvent.RemovedCard -> context.getString(
                R.string.person_removed_card, person, data.data.card?.name ?: context.getString(
                    R.string.inline_a_card
                ))
            CollaborationEvent.UpdatedCard -> {
                if (data.data.card == null) {
                    context.getString(R.string.person_updated_details, person, cardDetailName(data.data.details))
                } else {
                    context.getString(
                        R.string.person_updated_card, person, cardDetailName(data.data.details), data.data.card.name ?: context.getString(
                            R.string.inline_a_card
                        ))
                }
            }
        }
    }

    private fun personNameOrYou(person: Person?): String {
        return if (person?.id == meId) {
            context.getString(R.string.inline_you)
        } else {
            person?.name ?: context.getString(R.string.inline_someone)
        }
    }

    private fun cardDetailName(detail: CollaborationEventDataDetails?): String {
        return when (detail) {
            CollaborationEventDataDetails.Photo -> context.getString(R.string.inline_photo)
            CollaborationEventDataDetails.Video -> context.getString(R.string.inline_video)
            CollaborationEventDataDetails.Conversation -> context.getString(R.string.inline_group)
            CollaborationEventDataDetails.Name -> context.getString(R.string.inline_name)
            CollaborationEventDataDetails.Location -> context.getString(R.string.inline_hint)
            else -> ""
        }
    }

    private fun receive(data: MessagePushData) {
        if (
            latestEvent == Lifecycle.Event.ON_RESUME &&
            navController?.currentBackStackEntry?.destination?.route == "group/{id}" &&
            navController?.currentBackStackEntry?.arguments?.getString("id") == data.group.id
        ) {
            coroutineScope.launch {
                latestMessageFlow.emit(data.group.id)
            }
            return
        }

        val deeplinkIntent = Intent(
            Intent.ACTION_VIEW,
            "$appDomain/group/${data.group.id}".toUri(),
            context,
            MainActivity::class.java
        )

        send(
            intent = deeplinkIntent,
            channel = Notifications.Messages,
            groupKey = makeGroupKey(data.group.id!!),
            title = data.person.name ?: context.getString(R.string.someone),
            text = data.message.text?.nullIfBlank ?: data.message.attachmentText(context) ?: ""
        )
    }

    private fun send(
        intent: Intent,
        channel: Notifications,
        groupKey: String,
        title: String,
        text: String
    ) {
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        val builder = NotificationCompat.Builder(context, channel.key)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setGroup(groupKey)
            .setAutoCancel(true)
            .setContentIntent(TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(intent)
                    getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
            )

        notificationManager.notify(groupKey, 1, builder.build())
    }

    private fun makeGroupKey(groupId: String) = "group/$groupId"

    fun clear(groupId: String) {
        notificationManager.cancel(makeGroupKey(groupId), 1)
    }

    fun init(context: Context) {
        this.context = context

        CoroutineScope(Dispatchers.Default).launch {
            meId = context.dataStore.data.first()[meKey]
        }

        Notifications.values().forEach { channel ->
            val notificationChannel = NotificationChannel(
                channel.key,
                context.getString(channel.channelName),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = context.getString(channel.description)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}

enum class Notifications(@StringRes val channelName: Int, @StringRes val description: Int) {
    Messages(R.string.messages, R.string.messages_notification_channel_description),
    Collaboration(R.string.collaboration, R.string.collaboration_notification_channel_description);
    val key get() = name.lowercase()
}

enum class PushAction {
    Message,
    Collaboration
}

@Serializable
data class MessagePushData(
    val group: Group,
    val person: Person,
    val message: Message
)

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
}

@Serializable
data class CollaborationEventData (
    val card: Card? = null,
    val person: Person? = null,
    val details: CollaborationEventDataDetails? = null
)

@Serializable
data class CollaborationPushData(
    val person: Person,
    val card: Card,
    val event: CollaborationEvent,
    val data: CollaborationEventData,
)
