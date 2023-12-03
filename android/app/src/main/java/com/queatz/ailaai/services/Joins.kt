package com.queatz.ailaai.services

import app.ailaai.api.*
import com.queatz.ailaai.data.api
import com.queatz.db.JoinRequest
import com.queatz.db.JoinRequestAndPerson
import com.queatz.push.JoinRequestPushData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

val joins = Joins()

class Joins {

    val scope = CoroutineScope(Dispatchers.Default)

    val myJoins = MutableStateFlow<List<JoinRequestAndPerson>>(emptyList())
    val joins = MutableStateFlow<List<JoinRequestAndPerson>>(emptyList())

    suspend fun reload() {
        reloadIncoming()
        reloadMine()
    }

    private suspend fun reloadIncoming() {
        api.joinRequests {
            joins.value = it
        }
    }

    private suspend fun reloadMine() {
        api.myJoinRequests {
            myJoins.value = it
        }
    }

    suspend fun join(group: String, message: String) {
        api.newJoinRequest(JoinRequest(group = group, message = message))
        reloadMine()
    }

    suspend fun accept(joinRequest: String) {
        api.acceptJoinRequest(joinRequest)
        reloadIncoming()
    }

    suspend fun delete(joinRequest: String) {
        api.deleteJoinRequest(joinRequest)
        reloadIncoming()
        reloadMine()
    }

    fun onPush(data: JoinRequestPushData) {
        scope.launch {
            reloadIncoming()
            reloadMine()
        }
    }
}
