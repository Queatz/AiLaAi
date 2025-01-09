package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.at
import com.queatz.ailaai.extensions.hour
import com.queatz.ailaai.extensions.minute
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.pad
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random.Default.nextLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleDialog(
    onDismissRequest: () -> Unit,
    date: Instant,
    onUpdate: (Instant) -> Unit
) {
    val dateState = rememberDatePickerState(
        initialDisplayedMonthMillis = date.toEpochMilliseconds(),
        initialSelectedDateMillis = date.toEpochMilliseconds(),
        initialDisplayMode = DisplayMode.Input
    )
    val timeState = rememberTimePickerState(
        initialHour = date.hour(),
        initialMinute = date.minute()
    )

    var timeKey by rememberStateOf(0L)

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                key(timeKey) {
                    DatePicker(
                        state = dateState,
                        title = null,
                        colors = DatePickerDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                DateTimeSuggestions(
                    modifier = Modifier
                        .padding(horizontal = 3.pad)
                        .padding(bottom = 1.pad)
                ) {
                    dateState.displayedMonthMillis = it.toEpochMilliseconds()
                    dateState.selectedDateMillis = it.toEpochMilliseconds()
                    timeState.hour = it.hour()
                    timeState.minute = it.minute()
                    timeKey = nextLong()
                }
                key(timeKey) {
                    TimePicker(state = timeState)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onUpdate(
                            Instant.fromEpochMilliseconds(dateState.selectedDateMillis ?: 0)
                                .at(timeState.hour, timeState.minute)
                        )
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.update))
                }
            }
        }
    }
}
