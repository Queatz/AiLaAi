package com.queatz.ailaai.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun ScheduleItemActions(
    onDismissRequest: () -> Unit,
    onDone: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = PaddingDefault / 2)
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
                "Mark as done",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
//        OutlinedIconButton(
//            {
//                onDismissRequest()
//                onOpen()
//            },
//            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
//        ) {
//            Icon(
//                Icons.Outlined.OpenInNew,
//                "Open",
//                tint = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.size(18.dp)
//            )
//        }
        OutlinedIconButton(
            {
                onDismissRequest()
                onEdit()
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(
                Icons.Outlined.Edit,
                "Edit",
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
                "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
