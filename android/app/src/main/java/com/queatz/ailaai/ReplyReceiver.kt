package com.queatz.ailaai

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.ailaai.api.sendMessage
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.push
import com.queatz.db.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReplyReceiver : BroadcastReceiver() {

    val scope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            val replyText = remoteInput.getCharSequence(KEY_REPLY).toString()

            scope.launch(Dispatchers.IO) {
                api.sendMessage(
                    group = intent.getStringExtra(KEY_GROUP_ID)!!,
                    message = Message(
                        text = replyText
                    )
                ) {
                    withContext(Dispatchers.Main) {
                        push.clear(intent.getStringExtra(KEY_GROUP_ID)!!)
                        context.toast(R.string.message_sent)
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_REPLY = "reply"
        const val KEY_GROUP_ID = "groupId"
        const val KEY_NOTIFICATION_KEY = "notificationKey"
    }
}
