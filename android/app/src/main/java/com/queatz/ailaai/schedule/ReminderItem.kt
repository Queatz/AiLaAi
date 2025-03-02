package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder

@Composable
fun ReminderItem(reminder: Reminder) {
    val nav = nav
    OutlinedCard(
        onClick = {
            nav.appNavigate(AppNav.Reminder(reminder.id!!))
        },
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.pad)
        ) {
            Text(
                text = reminder.title ?: stringResource(R.string.new_reminder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = bulletedString(
                    reminder.categories?.firstOrNull(),
                    reminder.scheduleText,
                    reminder.note?.notBlank
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
