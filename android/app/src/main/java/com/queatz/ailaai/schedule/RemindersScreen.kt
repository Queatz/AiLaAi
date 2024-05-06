package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.reminders
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder
import java.util.Arrays

@Composable
fun RemindersScreen() {
    var isLoading by rememberStateOf(false)
    var reminders by rememberStateOf(emptyList<Reminder>())

    LaunchedEffect(Unit) {
        isLoading = true
        api.reminders {
            reminders = it
        }
        isLoading = false
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        AppBar(
            title = {
                Text(stringResource(R.string.your_reminders))
            },
            navigationIcon = {
                BackButton()
            }
        )

        if (isLoading) {
            Loading()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(1.pad),
                contentPadding = PaddingValues(bottom = 1.pad),
                modifier = Modifier.fillMaxSize()
                    .padding(horizontal = 1.pad)
            ) {
                items(reminders, key = { it.id!! }) {
                    ReminderItem(it)
                }
            }
        }
    }
}
