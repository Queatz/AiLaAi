package com.queatz.ailaai.push

import com.queatz.ailaai.services.Push
import com.queatz.push.MessageReactionPushData

fun Push.receive(data: MessageReactionPushData) {
    // Don't notify notifications from myself, allow content to refresh
    if (data.person?.id == meId) {
        return
    }

    // todo see message push
    // toto reload group, or show notification
}