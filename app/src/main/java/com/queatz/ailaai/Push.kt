package com.queatz.ailaai

val push = Push()

class Push {
    fun receive(data: Map<String, String>) {
        if (!data.containsKey("action")) {
            System.err.println("Push notification does not contain 'action'")
            return
        }

        try {
            when (PushAction.valueOf(data["action"]!!)) {
                PushAction.Message -> receive(gson.fromJson(data["data"]!!, MessagePushData::class.java)!!)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun receive(data: MessagePushData) {
        println("Got it! group=${data.group.id} person=${data.person.id} text=${data.message.text}")
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
