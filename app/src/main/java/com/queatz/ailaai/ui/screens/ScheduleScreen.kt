package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api.occurrences
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.Reminder
import com.queatz.ailaai.data.api
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.schedule.*
import com.queatz.ailaai.schedule.ScheduleView.*
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.ScanQrCodeButton
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private val viewKey = stringPreferencesKey("schedule.view")

@Composable
fun ScheduleScreen(navController: NavController, me: () -> Person?) {

    var events by rememberStateOf(emptyList<ReminderEvent>())
    val onExpand = remember { MutableSharedFlow<Unit>() }
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(true)
    var view by rememberStateOf(Monthly)
    val state = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        context.dataStore.data.first()[viewKey]?.let {
            runCatching {
                view = ScheduleView.valueOf(it)
            }
        }
    }

    val now = Clock.System.now().startOfDay()
    var offset by rememberStateOf(now)

    var showMenu by rememberStateOf(false)

    val updates = remember {
        MutableSharedFlow<Reminder>()
    }

    suspend fun reload() {
        val start = when (view) {
            Daily -> offset.startOfDay()
            Weekly -> offset.startOfWeek()
            Monthly -> offset.startOfMonth()
            Yearly -> offset.startOfYear()
        }

        api.occurrences(
            start,
            when (view) {
                Daily -> start.plus(days = Daily.range)
                Weekly -> start.plus(weeks = Weekly.range)
                Monthly -> start.plus(months = Monthly.range)
                Yearly -> start.plus(years = Yearly.range)
            }
        ) {
            events = buildList {
                it.forEach {
                    if (it.reminder.schedule == null) {
                        // Occurrences always override
                        if (it.occurrences.none { occurrence -> occurrence.occurrence == it.reminder.start }) {
                            add(
                                ReminderEvent(
                                    it.reminder,
                                    it.reminder.start!!,
                                    if (it.reminder.end == null) ReminderEventType.Occur else ReminderEventType.Start,
                                    null
                                )
                            )
                        }
                        if (it.reminder.end != null) {
                            // Occurrences always override
                            if (it.occurrences.none { occurrence -> occurrence.occurrence == it.reminder.end }) {
                                add(
                                    ReminderEvent(
                                        it.reminder,
                                        it.reminder.end!!,
                                        ReminderEventType.End,
                                        null
                                    )
                                )
                            }
                        }
                    }

                    it.occurrences.forEach { occurrence ->
                        if (occurrence.gone != true) {
                            add(
                                ReminderEvent(
                                    it.reminder,
                                    (occurrence.date ?: occurrence.occurrence)!!,
                                    when {
                                        it.reminder.schedule == null && it.reminder.end != null && it.reminder.start == occurrence.occurrence -> ReminderEventType.Start
                                        it.reminder.schedule == null && it.reminder.end != null && it.reminder.end == occurrence.occurrence -> ReminderEventType.End
                                        else -> ReminderEventType.Occur
                                    },
                                    occurrence
                                )
                            )
                        }
                    }

                    it.dates.filter { date ->
                        // Occurrences always override
                        it.occurrences.none { it.occurrence == date }
                    }.forEach { date ->
                        add(
                            ReminderEvent(
                                it.reminder,
                                date,
                                ReminderEventType.Occur,
                                null
                            )
                        )
                    }
                }
            }.sortedBy { it.date }
        }
        isLoading = false
    }

    LaunchedEffect(view, offset) {
        reload()
        updates.collectLatest {
            reload()
        }
    }

    fun scrollToTop() {
        scope.launch {
            state.scrollToTop()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                navController,
                stringResource(R.string.reminders),
                {
                    scrollToTop()
                },
                me
            ) {
                ScanQrCodeButton(navController)
                IconButton(
                    {
                        showMenu = true
                    }
                ) {
                    Icon(Icons.Outlined.MoreVert, null)
                    ScheduleMenu(
                        showMenu,
                        { showMenu = false },
                        view,
                        {
                            view = it
                            scope.launch {
                                context.dataStore.edit {
                                    it[viewKey] = view.toString()
                                }
                            }
                            events = emptyList()
                            isLoading = true
                            scrollToTop()
                        }
                    )
                }
            }
            if (isLoading) {
                Loading()
            } else {
                LazyColumn(
                    state = state,
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = PaddingDefault,
                        end = PaddingDefault,
                        top = PaddingDefault,
                        bottom = PaddingDefault + 80.dp
                    )
                ) {
                    var today = offset
                    (0 until view.range).forEach { period ->
                        val start = when (view) {
                            Daily -> today.startOfDay()
                            Weekly -> today.startOfWeek()
                            Monthly -> today.startOfMonth()
                            Yearly -> today.startOfYear()
                        }

                        val end = when (view) {
                            Daily -> start.plus(days = 1)
                            Weekly -> start.plus(weeks = 1)
                            Monthly -> start.plus(months = 1)
                            Yearly -> start.plus(years = 1)
                        }

                        today = end

                        Period(
                            view,
                            start,
                            end,
                            events.filter {
                                it.date >= start && it.date < end
                            },
                            onExpand = onExpand,
                            onUpdated = {
                                scope.launch { updates.emit(it.reminder) }
                            }
                        )
                    }
                }
            }
        }
        AddReminderLayout {
            updates.emit(it)
        }
    }
}
