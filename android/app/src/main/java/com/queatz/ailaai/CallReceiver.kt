package com.queatz.ailaai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.queatz.ailaai.CallService.Companion.CALL_NOTIFICATION_ID
import com.queatz.ailaai.CallService.Companion.GROUP_ID_EXTRA
import com.queatz.ailaai.services.calls

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationManagerCompat.from(context).cancel(CALL_NOTIFICATION_ID)
        calls.end(intent.getStringExtra(GROUP_ID_EXTRA))
    }
}
