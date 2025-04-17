package com.queatz.ailaai.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.pad

@Composable
fun ScheduleItemActions(
    onDismissRequest: () -> Unit,
    showOpen: Boolean,
    onDone: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onReschedule: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = .5f.pad)
    ) {
        OutlinedIconButton(
            {
                onDismissRequest()
                onDone()
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(
                Icons.Outlined.Done,
                stringResource(R.string.mark_as_done),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (showOpen) {
            OutlinedIconButton(
                {
                    onDismissRequest()
                    onOpen()
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    Icons.Outlined.OpenInNew,
                    stringResource(R.string.open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        OutlinedIconButton(
            {
                onDismissRequest()
                onReschedule()
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(
                Icons.Outlined.Update,
                stringResource(R.string.reschedule),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        OutlinedIconButton(
            {
                onDismissRequest()
                onEdit()
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(
                Icons.Outlined.Edit,
                stringResource(R.string.edit),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        OutlinedIconButton(
            {
                onDismissRequest()
                onRemove()
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(
                Icons.Outlined.Delete,
                stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
