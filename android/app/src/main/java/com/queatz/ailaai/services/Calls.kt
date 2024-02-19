package com.queatz.ailaai.services

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import com.queatz.ailaai.CallActivity
import com.queatz.ailaai.CallActivity.Companion.GROUP_ID_EXTRA

val calls by lazy {
    Calls()
}

class Calls {

    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context
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
