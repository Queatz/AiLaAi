package com.queatz.ailaai.schedule

import ReminderEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.theme.pad
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
        val context = LocalContext.current
        Text(
            start.formatTitle(context, view),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(
                    start = 1.pad,
                    end = 1.pad,
                    top = 2.pad,
                    bottom = 1.pad,
                )
        )
    }
    if (events.isEmpty()) {
        item(contentType = 1) {
            PeriodEmpty()
        }
    } else {
        itemsIndexed(events, key = { _, it -> "${it.reminder.id}:${it.date}:${it.occurrence?.id}" }) { index, event ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .let {
                        when {
                            index == 0 && index == events.lastIndex -> it.clip(MaterialTheme.shapes.large)
                            index == 0 -> it.clip(
                                MaterialTheme.shapes.large.copy(
                                    bottomStart = CornerSize(0.dp),
                                    bottomEnd = CornerSize(0.dp)
                                )
                            )

                            index == events.lastIndex -> it.clip(
                                MaterialTheme.shapes.large.copy(
                                    topStart = CornerSize(0.dp),
                                    topEnd = CornerSize(0.dp)
                                )
                            )

                            else -> it
                        }
                    }
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
            ) {
                PeriodDateTime(view, event.date, event.occurrence?.done == true)
                PeriodEvent(
                    view,
                    event,
                    showOpen = true,
                    showFullTime = false,
                    onExpand,
                    modifier = Modifier
                        .weight(1f),
                    onUpdated
                )
            }
        }
    }
}
