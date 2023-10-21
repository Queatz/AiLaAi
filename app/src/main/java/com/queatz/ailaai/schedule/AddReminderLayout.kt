package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.queatz.ailaai.R
import com.queatz.ailaai.api.newReminder
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.startOfMinute
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.Reminder
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun BoxScope.AddReminderLayout(onReminder: suspend (Reminder) -> Unit) {
    val scope = rememberCoroutineScope()
    var value by rememberStateOf("")
    var isAdding by rememberStateOf(false)

    fun addReminder() {
        scope.launch {
            isAdding = true
            api.newReminder(
                Reminder(
                    title = value.trim(),
                    start = Clock.System.now().startOfMinute()
                )
            ) {
                onReminder(it)
                value = ""
            }
            isAdding = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(
                PaddingDefault * 2,
            )
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
        FloatingActionButton(
            onClick = {
                if (isAdding) {
                    return@FloatingActionButton
                }
                if (value.isNotBlank()) {
                    addReminder()
                } else {
                    // todo go to all reminders screen
                }
            },
            modifier = Modifier
                .padding(
                    start = PaddingDefault * 2,
                )
        ) {
            if (value.isNotBlank()) {
                Icon(Icons.Outlined.Add, stringResource(R.string.add_reminder))
            } else {
                Icon(Icons.Outlined.Edit, stringResource(R.string.edit_reminders))
            }
        }
    }
}
