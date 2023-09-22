package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.datetime.Instant

@Composable
fun RowScope.PeriodDateTime(view: ScheduleView, date: Instant) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
            .padding(
                vertical = PaddingDefault,
            )
    ) {
        Text(
            date.formatDateTime(view),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium
        )
        when (view) {
            ScheduleView.Daily -> {}
            else -> {
                Text(
                    date.formatDateTimeHint(view),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
