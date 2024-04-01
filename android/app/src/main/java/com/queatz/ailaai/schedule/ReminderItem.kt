package com.queatz.ailaai.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder

@Composable
fun ReminderItem(reminder: Reminder) {
    val nav = nav
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable {
                nav.navigate(AppNav.Reminder(reminder.id!!))
            }
            .padding(1.pad)
    ) {
        Text(
            reminder.title ?: stringResource(R.string.add_reminder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            reminder.scheduleText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        if (!reminder.note.isNullOrBlank()) {
            Text(
                reminder.note!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
