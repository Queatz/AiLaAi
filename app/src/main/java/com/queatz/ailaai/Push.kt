package com.queatz.ailaai

import android.util.Log

val push = Push()

class Push {
    fun receive(data: Map<String, String>) {
        if (!data.containsKey("action")) {
            Log.w("PUSH", "Push notification does not contain 'action'")
            return
        }

        Log.w("PUSH", "Got push: ${data["action"]}")

        try {
            when (PushAction.valueOf(data["action"]!!)) {
                PushAction.Message -> receive(gson.fromJson(data["data"]!!, MessagePushData::class.java)!!)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun receive(data: MessagePushData) {
        Log.d("PUSH", "Got it! group=${data.group.id} person=${data.person.id} text=${data.message.text}")
    }
}

enum class PushAction {
    Message
}

data class MessagePushData(
    val group: Group,
    val person: Person,
    val message: Message
)
