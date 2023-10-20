package com.queatz.ailaai.services

import com.queatz.ailaai.api.*
import com.queatz.ailaai.data.JoinRequest
import com.queatz.ailaai.data.JoinRequestAndPerson
import com.queatz.ailaai.data.api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

val joins = Joins()

class Joins {

    private lateinit var scope: CoroutineScope

    val myJoins = MutableStateFlow<List<JoinRequestAndPerson>>(emptyList())
    val joins = MutableStateFlow<List<JoinRequestAndPerson>>(emptyList())

    fun start(scope: CoroutineScope) {
        this.scope = scope
        scope.launch {
            reload()
            reloadMine()
        }
    }

    suspend fun reload() {
        api.joinRequests {
            joins.emit(it)
        }
    }

    suspend fun reloadMine() {
        api.myJoinRequests {
            myJoins.emit(it)
        }
    }

    suspend fun join(group: String, message: String) {
        api.newJoinRequest(JoinRequest(group = group, message = message))
        reloadMine()
    }

    suspend fun accept(joinRequest: String) {
        api.acceptJoinRequest(joinRequest)
        reload()
    }

    suspend fun delete(joinRequest: String) {
        api.deleteJoinRequest(joinRequest)
        reload()
        reloadMine()
    }

    fun onPush(data: JoinRequestPushData) {
        scope.launch {
            reload()
            reloadMine()
        }
    }
}
