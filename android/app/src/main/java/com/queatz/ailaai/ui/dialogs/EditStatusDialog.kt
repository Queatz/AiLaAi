package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import app.ailaai.api.myStatus
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.fadingEdge
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.components.SetPhotoButton
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.PersonStatus
import com.queatz.db.Status
import kotlinx.coroutines.launch

@Composable
fun EditStatusDialog(
    onDismissRequest: () -> Unit,
    initialStatus: PersonStatus? = null,
    recentStatuses: List<Status>,
    onUpdated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var note by rememberStateOf(initialStatus?.note ?: "")
    var photo by rememberStateOf(initialStatus?.photo)
    var selectedStatus by rememberStateOf(initialStatus?.statusInfo)
    var customStatuses by rememberStateOf(emptyList<Status>())
    var isSaving by rememberStateOf(false)
    val me = me

    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = false,
            content = {
                val state = rememberLazyListState()
                var viewport by remember { mutableStateOf(Size(0f, 0f)) }
                var customStatusDialog by rememberStateOf(false)
                var showStatusHistory by rememberStateOf(false)

                LaunchedEffect(recentStatuses) {
                    if (selectedStatus != null) {
                        selectedStatus = (customStatuses + recentStatuses).find { it.id == selectedStatus?.id }
                    }
                }

                LaunchedEffect(note) {
                    if (showStatusHistory) {
                        showStatusHistory = false
                    }
                }

                if (customStatusDialog) {
                    CreateStatusDialog(
                        onDismissRequest = {
                            customStatusDialog = false
                        },
                        initialColor = Color.White
                    ) {
                        customStatuses = it.inList() + customStatuses
                        customStatusDialog = false
                        selectedStatus = it
                        scope.launch {
                            state.scrollToTop()
                        }
                    }
                }

                SetPhotoButton(
                    photoText = listOfNotNull(
                        selectedStatus?.name?.notBlank,
                        note.notBlank
                    ).joinToString(": "),
                    photo = photo.orEmpty(),
                    onRemove = if (photo == null) null else {
                        {
                            photo = null
                        }
                    },
                    modifier = Modifier.padding(bottom = 1.pad)
                ) {
                    photo = it
                }

                SearchField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = false,
                    placeholder = stringResource(R.string.note),
                    useMaxHeight = true,
                    useMaxWidth = false,
                    autoFocus = true,
                    endIcon = Icons.Outlined.History,
                    endIconTitle = stringResource(R.string.history),
                    onEndAction = {
                        showStatusHistory = !showStatusHistory
                    }
                )

                if (showStatusHistory) {
                    Statuses(
                        personId = me!!.id!!,
                        contentPadding = PaddingValues(vertical = 2.pad),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(vertical = 1.pad)
                            .fillMaxWidth()
                    ) { status ->
                        showStatusHistory = false
                        note = status.note.orEmpty()
                        photo = status.photo
                        selectedStatus = status.statusInfo
                        scope.launch {
                            state.scrollToTop()
                        }
                    }
                } else {
                    LazyColumn(
                        state = state,
                        modifier = Modifier
                            .padding(vertical = 1.pad)
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .onPlaced { viewport = it.boundsInParent().size }
                            .fadingEdge(viewport, state, 6f)
                    ) {
                        items((selectedStatus.inList() + customStatuses + recentStatuses).distinct()) { status ->
                            val selected = selectedStatus == status
                            StatusButton(
                                onClick = {
                                    if (selected) {
                                        selectedStatus = null
                                    } else {
                                        selectedStatus = status
                                    }
                                    scope.launch {
                                        state.scrollToTop()
                                    }
                                },
                                selected = selected,
                                status = status
                            )
                        }

                        item {
                            StatusButton(
                                onClick = {
                                    customStatusDialog = true
                                },
                                status = Status(
                                    name = stringResource(R.string.custom)
                                )
                            )
                        }
                    }
                }
            },
            actions = {
                if (initialStatus != null) {
                    TextButton(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                api.myStatus(PersonStatus()) {
                                    onUpdated()
                                }
                                isSaving = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.clear_status))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
                TextButton(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            api.myStatus(
                                PersonStatus(
                                    note = note,
                                    status = selectedStatus?.id,
                                    photo = photo?.notBlank
                                )
                            ) {
                                onUpdated()
                            }
                            isSaving = false
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text(stringResource(R.string.update))
                }
            }
        )
    }
}

@Composable
fun StatusButton(onClick: () -> Unit, selected: Boolean = false, status: Status) {
    TextButton(
        onClick = onClick,
        border = if (selected) ButtonDefaults.outlinedButtonBorder() else null,
        colors = if (selected) ButtonDefaults.elevatedButtonColors() else ButtonDefaults.textButtonColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .padding(.25f.pad)
                    .size(12.dp)
                    .then(
                        if (status.color != null) {
                            Modifier
                                .shadow(3.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(status.color!!.toColorInt()))
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = .5f),
                                            Color.White.copy(alpha = 0f)
                                        ),
                                        center = Offset(
                                            4.5f.dp.px.toFloat(),
                                            4.5f.dp.px.toFloat()
                                        ),
                                        radius = 9.dp.px.toFloat()
                                    ),
                                    shape = CircleShape
                                )
                        } else {
                            Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape)
                        }
                    )
                    .zIndex(1f)
            )
            Text(
                text = status.name.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground)
            )
        }
    }
}
