package com.queatz.ailaai.services

import app.ailaai.api.myDevice
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.queatz.ailaai.data.api
import com.queatz.db.DeviceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        if (token.isBlank()) {
            return
        }

        scope.launch {
            api.myDevice(DeviceType.Gms, token, onError = {})
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            push.got(message.data)
        }
    }
}
