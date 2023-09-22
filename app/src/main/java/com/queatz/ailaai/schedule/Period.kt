package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant

fun LazyListScope.Period(
    view: ScheduleView,
    start: Instant,
    end: Instant,
    events: List<ReminderEvent>,
    onExpand: MutableSharedFlow<Unit>,
    onUpdated: (ReminderEvent) -> Unit
) {
    item {
        Text(
            start.formatTitle(view),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(
                    start = PaddingDefault,
                    end = PaddingDefault,
                    bottom = PaddingDefault
                )
        )
    }
    if (events.isEmpty()) {
        item {
            PeriodEmpty()
        }
    } else {
        itemsIndexed(events) { index, event ->
            Row(
                verticalAlignment = Alignment.Top
            ) {
                PeriodDateTime(view, event.date)
                PeriodEvent(view, event, onExpand, onUpdated)
            }
        }
    }
}
