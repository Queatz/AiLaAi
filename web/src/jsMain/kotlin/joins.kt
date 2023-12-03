import app.ailaai.api.*
import com.queatz.db.JoinRequest
import com.queatz.db.JoinRequestAndPerson
import com.queatz.push.PushAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val joins = Joins()

class Joins {

    val myJoins = MutableStateFlow<List<JoinRequestAndPerson>>(emptyList())
    val joins = MutableStateFlow<List<JoinRequestAndPerson>>(emptyList())

    fun start(scope: CoroutineScope) {
        scope.launch {
            reload()
            push.events.filter {
                it.action == PushAction.JoinRequest ||
                it.action == PushAction.Group
            }.collectLatest {
                reload()
            }
        }
    }

    suspend fun reload() {
        reloadJoins()
        reloadMine()
    }

    private suspend fun reloadJoins() {
        application.bearerToken.first { it != null }
        api.joinRequests {
            joins.emit(it)
        }
    }

    private suspend fun reloadMine() {
        application.bearerToken.first { it != null }
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
        reloadJoins()
    }

    suspend fun delete(joinRequest: String) {
        api.deleteJoinRequest(joinRequest)
        reload()
    }
}
