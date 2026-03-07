package app.dialog

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import app.components.StatusName
import appString
import application
import com.queatz.db.PersonStatus
import com.queatz.db.Status
import components.GroupPhoto
import components.GroupPhotoItem
import components.IconButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import r

suspend fun editStatusDialog(
    statuses: List<Status>,
    initialStatus: PersonStatus? = null,
    onUpdate: (PersonStatus) -> Unit
) {
    val selected = MutableStateFlow(initialStatus?.statusInfo)
    val photo = MutableStateFlow(initialStatus?.photo)

    val note = inputWithListDialog(
        items = statuses,
        selected = selected,
        defaultValue = initialStatus?.note.orEmpty(),
        placeholder = application.appString { note },
        confirmButton = application.appString { update },
        topContent = {
            val currentPhoto = photo.collectAsState().value
            val choosePhotoControl = rememberChoosePhotoDialog(showUpload = true)
            val isGenerating = choosePhotoControl.isGenerating.collectAsState().value
            val scope = rememberCoroutineScope()

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    alignItems(AlignItems.Center)
                    gap(.5.r)
                    padding(0.r, 0.r, 1.r, 0.r)
                }
            }) {
                if (!currentPhoto.isNullOrBlank()) {
                    GroupPhoto(
                        listOf(GroupPhotoItem(currentPhoto, null)),
                        size = 64.px,
                        styles = {
                            marginRight(0.r)
                        },
                        onClick = if (isGenerating) null else {
                            {
                                scope.launch {
                                    choosePhotoControl.launch { it, _, _ ->
                                        photo.value = it
                                    }
                                }
                            }
                        }
                    )

                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Row)
                            alignItems(AlignItems.Center)
                            gap(1.r)
                        }
                    }) {
                        IconButton("photo", application.appString { this.choosePhoto }, isLoading = isGenerating, enabled = !isGenerating) {
                            scope.launch {
                                choosePhotoControl.launch { it, _, _ ->
                                    photo.value = it
                                }
                            }
                        }

                        IconButton("delete", application.appString { remove }, enabled = !isGenerating) {
                            photo.value = null
                        }
                    }
                } else {
                    IconButton("photo", application.appString { this.choosePhoto }, isLoading = isGenerating, enabled = !isGenerating) {
                        scope.launch {
                            choosePhotoControl.launch { it, _, _ ->
                                photo.value = it
                            }
                        }
                    }
                }
            }
        },
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
                status = selected.value?.id,
                photo = photo.value?.notBlank
            )
        )
    }
}
