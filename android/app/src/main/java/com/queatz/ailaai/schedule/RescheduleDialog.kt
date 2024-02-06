package com.queatz.ailaai.schedule

import ReminderEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.at
import com.queatz.ailaai.extensions.hour
import com.queatz.ailaai.extensions.minute
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.pad
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleDialog(onDismissRequest: () -> Unit, date: Instant, onUpdate: (Instant) -> Unit) {
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = date.toEpochMilliseconds(),
        initialDisplayMode = DisplayMode.Input
    )
    val timeState = rememberTimePickerState(
        initialHour = date.hour(),
        initialMinute = date.minute()
    )

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DatePicker(dateState, title = null)
                TimePicker(timeState)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    {
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
