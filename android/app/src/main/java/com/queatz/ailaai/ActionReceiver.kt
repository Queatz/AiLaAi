package com.queatz.ailaai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.ailaai.api.updateReminderOccurrence
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.services.push
import com.queatz.db.ReminderOccurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed class PushNotificationAction {
    @Serializable
    data class MarkAsDone(
        val reminder: String,
        val occurrence: Instant
    ) : PushNotificationAction()
}

class ActionReceiver : BroadcastReceiver() {

    val scope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val action = json.decodeFromString<PushNotificationAction>(intent.getStringExtra(KEY_ACTION).orEmpty())
        val notificationKey = intent.getStringExtra(KEY_NOTIFICATION_KEY).orEmpty()

        when (action) {
            is PushNotificationAction.MarkAsDone -> {
                scope.launch(Dispatchers.IO) {
                    api.updateReminderOccurrence(
                        id = action.reminder,
                        occurrence = action.occurrence,
                        update = ReminderOccurrence(done = true)
                    ) {
                        withContext(Dispatchers.Main) {
                            context.toast(context.getString(R.string.occurrence_marked_as_done))
                            push.clear(notificationKey)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_ACTION = "action"
        const val KEY_NOTIFICATION_KEY = "notificationKey"
    }
}
