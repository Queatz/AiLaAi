package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.reminders
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.sortedDistinct
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.Categories
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder

@Composable
fun RemindersScreen() {
    var isLoading by rememberStateOf(false)
    var reminders by rememberStateOf(emptyList<Reminder>())
    var search by rememberStateOf("")
    var selectedCategory by rememberStateOf<String?>(null)

    LaunchedEffect(Unit) {
        isLoading = true
        api.reminders {
            reminders = it
        }
        isLoading = false
    }

    val categories = remember(reminders) {
        reminders.mapNotNull { it.categories }.flatten().sortedDistinct()
    }

    val shownReminders = remember(reminders, search, selectedCategory) {
        if (search.isBlank()) {
            reminders
        } else {
            reminders.filter {
                it.title?.contains(search.trim(), ignoreCase = true) == true ||
                        it.note?.contains(search.trim(), ignoreCase = true) == true
            }
        }.let {
            if (selectedCategory == null) {
                it
            } else {
                it.filter { it.categories?.contains(selectedCategory) == true }
            }
        }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(1.pad),
                    contentPadding = PaddingValues(bottom = 1.pad + 80.dp),
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = 1.pad)
                ) {
                    items(shownReminders, key = { it.id!! }) {
                        ReminderItem(it)
                    }
                }
                PageInput(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    Categories(
                        categories = categories,
                        category = selectedCategory,
                        visible = categories.isNotEmpty()
                    ) {
                        selectedCategory = if (it == selectedCategory) {
                            null
                        } else {
                            it
                        }
                    }
                    SearchField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.padding(horizontal = 1.pad)
                    )
                }
            }
        }
    }
}
