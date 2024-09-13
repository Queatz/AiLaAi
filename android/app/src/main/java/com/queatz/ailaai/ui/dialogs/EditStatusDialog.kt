package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import app.ailaai.api.myStatus
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.SearchField
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
    var selectedStatus by rememberStateOf(initialStatus?.statusInfo)
    var customStatusDialog by rememberStateOf(false)
    var customStatuses by rememberStateOf(emptyList<Status>())
    var isSaving by rememberStateOf(false)

    LaunchedEffect(recentStatuses) {
        if (selectedStatus != null) {
            selectedStatus = (customStatuses + recentStatuses).find { it.id == selectedStatus?.id }
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
        }
    }

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
        ) {
            SearchField(
                value = note,
                onValueChange = { note = it },
                singleLine = false,
                placeholder = stringResource(R.string.note),
                useMaxHeight = true,
                autoFocus = true
            )
            (customStatuses + recentStatuses).forEach { status ->
                val selected = selectedStatus == status
                StatusButton(
                    onClick = {
                        if (selected) {
                            selectedStatus = null
                        } else {
                            selectedStatus = status
                        }
                    },
                    selected = selected,
                    status = status
                )
            }
            StatusButton(
                onClick = {
                    customStatusDialog = true
                },
                status = Status(
                    // todo: translate
                    name = stringResource(R.string.custom),
                    color = "#888888"
                )
            )
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                                    status = selectedStatus?.id
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
        }
    }
}

@Composable
fun StatusButton(onClick: () -> Unit, selected: Boolean = false, status: Status) {
    TextButton(
        onClick = onClick,
        border = if (selected) ButtonDefaults.outlinedButtonBorder() else null,
        colors = if (selected) ButtonDefaults.elevatedButtonColors() else ButtonDefaults.textButtonColors(),
        modifier = Modifier.fillMaxWidth(),
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
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color((status.color ?: "#ffffff").toColorInt()))
                    .zIndex(1f)
            )
            Text(
                text = status.name.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground)
            )
        }
    }
}
