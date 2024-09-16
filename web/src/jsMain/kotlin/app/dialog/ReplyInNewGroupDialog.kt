package app.dialog

import Strings.groupName
import api
import app.ailaai.api.createGroup
import app.ailaai.api.sendMessage
import app.ailaai.api.updateGroup
import app.group.friendsDialog
import app.messaages.inList
import app.nav.name
import application
import com.queatz.db.Group
import com.queatz.db.GroupAttachment
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

suspend fun replyInNewGroupDialog(
    title: String,
    scope: CoroutineScope,
    group: GroupExtended,
    message: Message,
    onGroup: suspend (Group) -> Unit,
) = friendsDialog(
    title = title,
    items = group.members?.mapNotNull { it.person } ?: emptyList(),
    preselect = group.members?.filter {
        it.member?.seen?.let { it >= message.createdAt!! } == true && it.person?.id != application.me.value?.id
    }?.map { it.person!!.id!! }?.toSet() ?: emptySet(),
    omit = application.me.value?.id?.inList() ?: emptyList(),
    multiple = true,
    confirmButton = application.appString { reply }
) {
    if (it.isNotEmpty()) {
        val groupName = group.name(
            someone = application.appString { someone },
            emptyGroup = application.appString { newGroup },
            omit = application.me.value?.id?.inList() ?: emptyList()
        )
        scope.launch {
            api.createGroup(it.map { it.id!! }) {
                api.updateGroup(it.id!!, Group(description = "$groupName / $title")) { newGroup ->
                    api.sendMessage(newGroup.id!!, message = Message(
                        attachment = json.encodeToString(GroupAttachment(group = group.group!!.id!!))
                    ))
                    onGroup(newGroup)
                }
            }
        }
    }
}
