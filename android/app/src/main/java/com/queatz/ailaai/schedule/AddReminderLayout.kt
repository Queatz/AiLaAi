package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.ailaai.api.newReminder
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.startOfMinute
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.FloatingButton
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

@Composable
fun AddReminderLayout(modifier: Modifier = Modifier, onReminder: suspend (Reminder) -> Unit) {
    val scope = rememberCoroutineScope()
    var value by rememberStateOf("")
    var isAdding by rememberStateOf(false)
    var showScheduleReminder by rememberStateOf(false)
    val keyboardController = LocalSoftwareKeyboardController.current!!
    val nav = nav

    fun addReminder(reminder: Reminder? = null) {
        scope.launch {
            isAdding = true
            api.newReminder(
                Reminder(
                    title = value.trim(),
                    start = reminder?.start ?: Clock.System.now().startOfMinute(),
                    end = reminder?.end,
                    schedule = reminder?.schedule,
                    timezone = TimeZone.currentSystemDefault().id,
                    utcOffset = TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds / (60.0 * 60.0),
                )
            ) {
                onReminder(it)
                value = ""
            }
            isAdding = false
            keyboardController.hide()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(2.pad)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth()
        ) {
            SearchField(
                value,
                { value = it },
                stringResource(R.string.add_reminder),
                showClear = false,
                imeAction = ImeAction.Done,
                onAction = {
                    addReminder()
                }
            )
        }
        FloatingButton(
            onClick = {
                if (isAdding) {
                    return@FloatingButton
                }
                if (value.isNotBlank()) {
                    addReminder()
                } else {
                    nav.navigate("reminders")
                }
            },
            onLongClick = {
                if (isAdding) {
                    return@FloatingButton
                }
                if (value.isNotBlank()) {
                    showScheduleReminder = true
                }
            },
            onClickLabel = stringResource(R.string.add_reminder),
            onLongClickLabel = stringResource(R.string.schedule_reminder),
            modifier = Modifier.padding(start = 2.pad)
        ) {
            if (value.isNotBlank()) {
                Box {
                    Icon(Icons.Outlined.Add, stringResource(R.string.add_reminder))
                    Icon(Icons.Outlined.Schedule, stringResource(R.string.schedule_reminder), modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.TopEnd)
                        .offset(8.dp, -8.dp)
                        .alpha(.75f)
                    )
                }
            } else {
                Icon(Icons.Outlined.Edit, stringResource(R.string.edit_reminders))
            }
        }
    }

    if (showScheduleReminder) {
        ScheduleReminderDialog(
            {
                showScheduleReminder = false
            },
            initialReminder = Reminder(
                start = Clock.System.now().startOfMinute()
            ),
            confirmText = stringResource(R.string.add_reminder)
        ) {
            addReminder(it)
            showScheduleReminder = false
        }
    }
}
