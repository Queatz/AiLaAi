package com.queatz.ailaai.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.os.bundleOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.ReplyReceiver
import com.queatz.ailaai.ReplyReceiver.Companion.KEY_REPLY
import com.queatz.ailaai.data.json
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.push.receive
import com.queatz.db.Bot
import com.queatz.db.Person
import com.queatz.push.CallPushData
import com.queatz.push.CallStatusPushData
import com.queatz.push.CollaborationPushData
import com.queatz.push.CommentPushData
import com.queatz.push.CommentReplyPushData
import com.queatz.push.GroupPushData
import com.queatz.push.JoinRequestPushData
import com.queatz.push.MessagePushData
import com.queatz.push.MessageReactionPushData
import com.queatz.push.PushAction
import com.queatz.push.PushDataData
import com.queatz.push.ReminderPushData
import com.queatz.push.StoryPushData
import com.queatz.push.TradePushData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val push by lazy {
    Push()
}

class Push {

    internal lateinit var context: Context
    var navController: NavController? = null
    var latestEvent: Lifecycle.Event? = null
    val scope = CoroutineScope(Dispatchers.Default)
    private val meKey = stringPreferencesKey("me")
    internal var meId: String? = null

    internal val latestMessageFlow = MutableSharedFlow<String?>()
    val latestMessage: Flow<String?> = latestMessageFlow

    private val eventsFlow = MutableSharedFlow<PushDataData>()
    val events: Flow<PushDataData> = eventsFlow

    private val notificationManager by lazy {
        NotificationManagerCompat.from(context)
    }

    suspend fun setMe(id: String) {
        meId = id
        context.dataStore.edit {
            it[meKey] = id
        }
    }

    fun got(data: Map<String, String>) {
        if (!data.containsKey("action")) {
            Log.w("PUSH", "Push notification does not contain 'action'")
            return
        }

        val action = data["action"]!!
        val push = data["data"]!!

        try {
            // todo isn't this automatic already?
            when (PushAction.valueOf(action)) {
                PushAction.Message -> receive(parse<MessagePushData>(push))
                PushAction.MessageReaction -> receive(parse<MessageReactionPushData>(push))
                PushAction.Collaboration -> receive(parse<CollaborationPushData>(push))
                PushAction.JoinRequest -> receive(parse<JoinRequestPushData>(push))
                PushAction.Group -> receive(parse<GroupPushData>(push))
                PushAction.Call -> receive(parse<CallPushData>(push))
                PushAction.CallStatus -> receive(parse<CallStatusPushData>(push))
                PushAction.Reminder -> receive(parse<ReminderPushData>(push))
                PushAction.Trade -> receive(parse<TradePushData>(push))
                PushAction.Story -> receive(parse<StoryPushData>(push))
                PushAction.Comment -> receive(parse<CommentPushData>(push))
                PushAction.CommentReply -> receive(parse<CommentReplyPushData>(push))
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    private inline fun <reified T : PushDataData> parse(action: String): T =
        json.decodeFromString<T>(action).also {
            scope.launch {
                eventsFlow.emit(it)
            }
        }

    internal fun personNameOrYou(person: Person?, bot: Bot? = null, inline: Boolean = true): String {
        return if (person?.id == meId) {
            context.getString(if (inline) R.string.inline_you else R.string.you)
        } else if (person != null) {
            person.name ?: context.getString(if (inline) R.string.inline_someone else R.string.someone)
        } else if (bot != null) {
            bot.name ?: context.getString(R.string.app_name)
        } else {
            context.getString(R.string.app_name)
        }
    }

    @SuppressLint("MissingPermission")
    internal fun send(
        intent: Intent,
        channel: Notifications,
        groupKey: String,
        title: String,
        text: String,
        style: NotificationCompat.Style? = null,
        sound: Uri? = null,
        alarm: Boolean = false,
        replyInGroup: String? = null
    ) {
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        val builder = NotificationCompat.Builder(context, channel.key)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style ?: NotificationCompat.BigTextStyle().bigText(text))
            .setGroup(groupKey)
            .setAutoCancel(true)
            .setPriority(channel.importance.priority)
            .setCategory(channel.category)
            .setContentIntent(
                TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(intent)
                    getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT or (if (replyInGroup == null) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_MUTABLE)
                    )
                }
            ).let {
                if (sound != null) {
                    it.setSound(sound)
                } else if (alarm) {
                    val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    it.setSound(alarmSoundUri)
                } else {
                    it
                }
            }.let {
                if (replyInGroup != null) {
                    val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
                        putExtras(
                            bundleOf(
                                ReplyReceiver.KEY_GROUP_ID to replyInGroup,
                                ReplyReceiver.KEY_NOTIFICATION_KEY to groupKey,
                            )
                        )
                    }
                    val replyPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )

                    val remoteInput = RemoteInput.Builder(KEY_REPLY)
                        .setLabel(context.getString(R.string.reply))
                        .build()

                    val replyAction = NotificationCompat.Action.Builder(
                        null,
                        context.getString(R.string.reply),
                        replyPendingIntent
                    ).addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build()

                    it.addAction(
                        replyAction
                    )
                } else {
                    it
                }
            }

        notificationManager.notify(groupKey, 1, builder.build())
    }

    internal fun makeGroupKey(groupId: String) = "group/$groupId"

    fun clear(groupId: String) {
        notificationManager.cancel(makeGroupKey(groupId), 1)
    }

    fun init(context: Context) {
        this.context = context

        CoroutineScope(Dispatchers.Default).launch {
            meId = context.dataStore.data.first()[meKey]
        }

        Notifications.entries.forEach { channel ->
            val notificationChannel = NotificationChannel(
                channel.key,
                context.getString(channel.channelName),
                channel.importance
            )
            notificationChannel.description = context.getString(channel.description)

            if (channel.alarm) {
                val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                notificationChannel.setSound(alarmSoundUri, audioAttributes)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}

enum class Notifications(
    @StringRes val channelName: Int,
    @StringRes val description: Int,
    val importance: Int,
    val category: String,
    val alarm: Boolean = false
) {
    Calls(
        R.string.calls,
        R.string.calls_notification_channel_description,
        NotificationManager.IMPORTANCE_HIGH,
        NotificationCompat.CATEGORY_CALL
    ),
    OngoingCalls(
        R.string.ongoing_calls,
        R.string.calls_notification_channel_description,
        NotificationManager.IMPORTANCE_LOW,
        NotificationCompat.CATEGORY_CALL
    ),
    Reminders(
        R.string.reminders,
        R.string.reminders_notification_channel_description,
        NotificationManager.IMPORTANCE_HIGH,
        NotificationCompat.CATEGORY_REMINDER
    ),
    Alarms(
        R.string.alarms,
        R.string.reminders_notification_channel_description,
        NotificationManager.IMPORTANCE_MAX,
        NotificationCompat.CATEGORY_ALARM,
        alarm = true
    ),
    Messages(
        R.string.messages,
        R.string.messages_notification_channel_description,
        NotificationManager.IMPORTANCE_HIGH,
        NotificationCompat.CATEGORY_MESSAGE
    ),
    Host(
        R.string.host,
        R.string.host_notification_channel_description,
        NotificationManager.IMPORTANCE_DEFAULT,
        NotificationCompat.CATEGORY_SOCIAL
    ),
    Collaboration(
        R.string.collaboration,
        R.string.collaboration_notification_channel_description,
        NotificationManager.IMPORTANCE_DEFAULT,
        NotificationCompat.CATEGORY_SOCIAL
    ),
    Trade(
        R.string.trade,
        R.string.trade_notification_channel_description,
        NotificationManager.IMPORTANCE_HIGH,
        NotificationCompat.CATEGORY_SOCIAL
    ),
    Subscriptions(
        R.string.subscriptions,
        R.string.subscription_notification_channel_description,
        NotificationManager.IMPORTANCE_DEFAULT,
        NotificationCompat.CATEGORY_SOCIAL
    ),
    Comments(
        R.string.comments,
        R.string.comments_notification_channel_description,
        NotificationManager.IMPORTANCE_DEFAULT,
        NotificationCompat.CATEGORY_SOCIAL
    ),
    ;

    val key get() = name.lowercase()
}

val Int.priority: Int
    get() = when (this) {
        NotificationManager.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
        NotificationManager.IMPORTANCE_LOW -> NotificationCompat.PRIORITY_LOW
        else -> NotificationCompat.PRIORITY_DEFAULT
    }

fun Push.isTopGroup(groupId: String?) = latestEvent == Lifecycle.Event.ON_RESUME &&
        navController?.currentBackStackEntry?.destination?.route == "group/{id}" &&
        navController?.currentBackStackEntry?.arguments?.getString("id") == groupId
