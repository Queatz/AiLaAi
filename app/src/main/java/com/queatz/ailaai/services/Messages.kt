package com.queatz.ailaai.services

import com.queatz.ailaai.data.GroupExtended
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.extensions.isUnread
import kotlinx.coroutines.flow.MutableStateFlow

val messages by lazy {
    Messages()
}

class Messages {
    fun refresh(me: Person?, groups: List<GroupExtended>) {
        new.value = groups.count {
            it.isUnread(
                it.members?.find { it.person?.id == me?.id }?.member
            )
        }
    }

    fun clear() {
        new.value = 0
    }

    val new = MutableStateFlow(0)
}
