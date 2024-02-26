package com.queatz.ailaai.push

import android.annotation.SuppressLint
import android.content.Intent
import androidx.core.os.bundleOf
import com.queatz.ailaai.CallService.Companion.GROUP_ID_EXTRA
import com.queatz.ailaai.CallService.Companion.GROUP_NAME_EXTRA
import com.queatz.ailaai.IncomingCallService
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.services.Push
import com.queatz.push.CallPushData


@SuppressLint("MissingPermission")
fun Push.receive(data: CallPushData) {
    val title = data.group.name?.notBlank ?: data.person.name?.notBlank ?: context.getString(R.string.someone)

    if (data.show != false) {
        context.startForegroundService(
            Intent(
                context,
                IncomingCallService::class.java
            ).putExtras(
                bundleOf(
                    GROUP_ID_EXTRA to data.group.id!!,
                    GROUP_NAME_EXTRA to title
                )
            )
        )
    }
}
