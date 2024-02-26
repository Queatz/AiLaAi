package com.queatz.ailaai

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class CallService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, intentFlags: Int, startId: Int): Int {
        //todo (need to handle restarting of sticky notification)
        intent ?: return START_NOT_STICKY

        val groupId = intent.getStringExtra(GROUP_ID_EXTRA) ?: error("Missing '$GROUP_ID_EXTRA'")
        val groupName = intent.getStringExtra(GROUP_NAME_EXTRA) ?: error("Missing '$GROUP_NAME_EXTRA'")

        // Show ongoing call notification
        val callIntent = Intent(
            applicationContext,
            CallActivity::class.java
        ).apply {
            action = Intent.ACTION_CALL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val endCallIntent = Intent(
            applicationContext,
            CallReceiver::class.java
        ).putExtras(
            bundleOf(
                GROUP_ID_EXTRA to groupId
            )
        )

        val notification = NotificationCompat.Builder(this, Notifications.OngoingCalls.key)
            .setPriority(Notifications.OngoingCalls.importance.priority)
            .setCategory(Notifications.OngoingCalls.category)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.call_is_active))
            .setContentText(applicationContext.getString(R.string.tap_to_return_to_call))
            .setOngoing(true)
            .setSound(null)
            .setContentIntent(
                TaskStackBuilder.create(applicationContext).run {
                    addNextIntentWithParentStack(callIntent)
                    getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
            )
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    Person.Builder()
                        .setName(groupName)
                        .setImportant(true)
                        .build(),
                    PendingIntent.getBroadcast(applicationContext, 0, endCallIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )
            .build()

        try {
            ServiceCompat.startForeground(
                this,
                CALL_NOTIFICATION_ID,
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

        // Join the call
        calls.start(
            groupId,
            applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
            applicationContext.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) {
            // Show call
            applicationContext.startActivity(
                Intent(
                    applicationContext,
                    CallActivity::class.java
                ).apply {
                    action = Intent.ACTION_CALL
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )

            scope.launch {
                calls.onEndCall.filter { it == groupId }.collectLatest {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        try {
            scope.cancel()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent) = null

    companion object {
        const val GROUP_ID_EXTRA = "groupId"
        const val GROUP_NAME_EXTRA = "groupName"
        const val CALL_NOTIFICATION_ID = 8888
    }
}
