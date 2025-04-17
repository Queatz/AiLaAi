package app.dialog

import app.components.StatusName
import appString
import application
import com.queatz.db.PersonStatus
import com.queatz.db.Status
import components.IconButton
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import r

suspend fun editStatusDialog(
    statuses: List<Status>,
    initialStatus: PersonStatus? = null,
    onUpdate: (PersonStatus) -> Unit
) {
    var selected = MutableStateFlow(initialStatus?.statusInfo)

    val note = inputWithListDialog(
        items = statuses,
        selected = selected,
        defaultValue = initialStatus?.note.orEmpty(),
        placeholder = application.appString { note },
        confirmButton = application.appString { update },
        extraButtons = { resolve ->
            IconButton(
                name = "delete",
                title = appString { clearStatus }
            ) {
                onUpdate(PersonStatus())
                resolve(false)
            }
        },
        isSelected = {
            selected.value?.id == it.id
        },
        onSelect = {
            if (selected.value?.id == it.id) {
                selected.value = null
            } else {
                selected.value = it
            }
        },
        itemContent = {
            Div({
                style {
                    padding(.25.r, 0.r)
                }
            }) {
                StatusName(it, gap = .5.r)
            }
        }
    )

    if (note != null) {
        onUpdate(
            PersonStatus(
                note = note,
                status = selected.value?.id
            )
        )
    }
}
