package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.theme.pad
import kotlin.time.Instant

@Composable
fun PeriodDateTime(view: ScheduleView, date: Instant, done: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
            .padding(vertical = 1.pad)
    ) {
        Text(
            date.formatDateTime(view),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (done) .5f else 1f),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            date.formatDateTimeHint(view),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = if (done) .5f else 1f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
