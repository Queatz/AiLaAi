package com.queatz.ailaai.services

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import app.ailaai.api.calls
import com.queatz.ailaai.CallActivity
import com.queatz.ailaai.CallActivity.Companion.GROUP_ID_EXTRA
import com.queatz.ailaai.data.api
import com.queatz.db.Call
import com.queatz.push.CallStatusPushData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val calls by lazy {
    Calls()
}

class Calls {

    private lateinit var context: Context

    val calls = MutableStateFlow<List<Call>>(emptyList())

    val scope = CoroutineScope(Dispatchers.Default)

    fun init(context: Context) {
        this.context = context

        scope.launch {
            reload()
            push.events.filter {
                it is CallStatusPushData
            }.collectLatest {
                reload()
            }
        }
    }

    suspend fun reload() {
        api.calls { calls ->
            this.calls.update {
                calls
            }
        }
    }

    fun join(groupId: String) {
        context.startActivity(
            Intent(
                context,
                CallActivity::class.java
            ).apply {
                action = Intent.ACTION_CALL
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtras(
                    bundleOf(
                        GROUP_ID_EXTRA to groupId
                    )
                )
            }
        )
    }
}
