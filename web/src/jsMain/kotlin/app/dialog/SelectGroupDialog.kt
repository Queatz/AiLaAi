package app.dialog

import Configuration
import api
import app.ailaai.api.groups
import app.group.GroupInfo
import app.group.GroupItem
import application
import com.queatz.db.GroupExtended

suspend fun selectGroupDialog(
    configuration: Configuration,
    title: String = application.appString { inAGroup },
    cancelButton: String? = null
): GroupExtended? {
    var selectedGroup: GroupExtended? = null
    searchDialog(
        configuration = configuration,
        title = title,
        confirmButton = application.appString { cancel },
        cancelButton = cancelButton,
        load = {
            var groups = emptyList<GroupExtended>()
            api.groups {
                groups = it
            }
            groups
        },
        filter = { it, value ->
            (it.group?.name?.contains(value, true)
                ?: false) || (it.members?.any { it.person?.name?.contains(value, true) ?: false } ?: false)
        }
    ) { it, resolve ->
        GroupItem(
            it,
            selectable = true,
            onSelected = {
                selectedGroup = it
                resolve(false)
            },
            info = GroupInfo.LatestMessage
        )
    }
    return selectedGroup
}
