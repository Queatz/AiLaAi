package com.queatz.ailaai

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.ServiceCompat
import androidx.core.os.bundleOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.calls
import com.queatz.ailaai.services.priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class IncomingCallService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        val groupId =
            intent.getStringExtra(CallService.GROUP_ID_EXTRA) ?: error("Missing '${CallService.GROUP_ID_EXTRA}'")
        val groupName =
            intent.getStringExtra(CallService.GROUP_NAME_EXTRA) ?: error("Missing '${CallService.GROUP_NAME_EXTRA}'")

        val context = applicationContext

        val callIntent = Intent(context, CallService::class.java).apply {
            putExtras(
                bundleOf(
                    CallService.GROUP_ID_EXTRA to groupId,
                    CallService.GROUP_NAME_EXTRA to groupName,
                )
            )
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: Uri.parse("android.resource://${context.packageName}/${R.raw.call}")

        val intent = PendingIntent.getForegroundService(
            context,
            0,
            callIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val endCallIntent = Intent(
            context,
            CallReceiver::class.java
        ).putExtras(
            bundleOf(
                CallService.GROUP_ID_EXTRA to groupId
            )
        )

        val channel = Notifications.Calls
        val notification = NotificationCompat.Builder(context, channel.key)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(groupName)
            .setContentText(context.getString(R.string.tap_to_answer))
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    Person.Builder()
                        .setName(groupName)
                        .setImportant(true)
                        .build(),
                    PendingIntent.getBroadcast(context, 0, endCallIntent, PendingIntent.FLAG_IMMUTABLE),
                    intent
                )
            )
            .setOngoing(true)
            .setSound(soundUri)
            .setPriority(channel.importance.priority)
            .setCategory(channel.category)
            .setContentIntent(intent)
            .build()

        try {
            ServiceCompat.startForeground(
                this,
                CallService.CALL_NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                } else {
                    0
                }
            )
        } catch (t: Throwable) {
            t.printStackTrace()
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && t is ForegroundServiceStartNotAllowedException
            ) {
                applicationContext.showDidntWork()
            }
        }

        scope.launch {
            calls.onEndCall.filter { it == groupId }.collectLatest {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?) = null
}
