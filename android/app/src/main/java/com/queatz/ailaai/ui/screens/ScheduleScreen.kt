package com.queatz.ailaai.ui.screens

import ReminderEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.ailaai.api.occurrences
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.nav
import com.queatz.ailaai.schedule.*
import com.queatz.ailaai.schedule.ScheduleView.*
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

private val viewKey = stringPreferencesKey("schedule.view")

private var cache = emptyList<ReminderEvent>()

@Composable
fun ScheduleScreen() {
    var events by rememberStateOf(cache)
    val onExpand = remember { MutableSharedFlow<Unit>() }
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(true)
    var view by rememberStateOf(Monthly)
    val state = rememberLazyListState()
    val context = LocalContext.current
    val nav = nav

    LaunchedEffect(events) {
        cache = events
    }

    LaunchedEffect(Unit) {
        context.dataStore.data.first()[viewKey]?.let {
            runCatching {
                view = ScheduleView.valueOf(it)
            }
        }
    }

    fun initialRange(): Pair<Instant, Instant> {
        val now = Clock.System.now().startOfDay()

        return when (view) {
            Daily -> now.startOfDay()
            Weekly -> now.startOfWeek()
            Monthly -> now.startOfMonth()
            Yearly -> now.startOfYear()
        } to when (view) {
            Daily -> now.plus(days = Daily.range)
            Weekly -> now.plus(weeks = Weekly.range)
            Monthly -> now.plus(months = Monthly.range)
            Yearly -> now.plus(years = Yearly.range)
        }
    }

    var showMenu by rememberStateOf(false)
    var range by remember(view) {
        mutableStateOf(
            initialRange()
        )
    }
    var shownRange by rememberStateOf(range)

    val updates = remember {
        MutableSharedFlow<Reminder>()
    }

    suspend fun reload() {
        val range = range
        api.occurrences(
            range.first,
            range.second
        ) {
            isLoading = false
            val scrollToTop = range.first != shownRange.first
            shownRange = range
            // todo move this list to :shared
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

            if (scrollToTop) {
                delay(100)
                state.layoutInfo.visibleItemsInfo.firstOrNull { it.contentType != -1 }?.index?.let {
                    state.animateScrollToItem(
                        it,
                        0
                    )
                }
            }
        }
    }

    LaunchedEffect(view, range) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeMainTabs {
                when (emptyList<Unit>().swipe(Unit, it)) {
                    is SwipeResult.Previous -> {
                        nav.navigate(AppNav.Messages)
                    }
                    is SwipeResult.Next -> {
                        nav.navigate(AppNav.Explore)
                    }
                    is SwipeResult.Select<*> -> {
                        // Impossible
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                stringResource(R.string.reminders),
                {
                    scrollToTop()
                },
            ) {
                IconButton(
                    {
                        showMenu = true
                    }
                ) {
                    Icon(Icons.Outlined.MoreVert, null)
                    ScheduleMenu(
                        showMenu,
                        { showMenu = false },
                        view = view,
                        onView = {
                            if (view == it) {
                                range = initialRange()
                            } else {
                                view = it
                                scope.launch {
                                    context.dataStore.edit {
                                        it[viewKey] = view.toString()
                                    }
                                }
                                events = emptyList()
                                isLoading = true
                            }
                            scrollToTop()
                        }
                    )
                }
                ScanQrCodeButton()
            }
            NotificationsDisabledBanner()
            if (isLoading) {
                Loading()
            } else {
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 1.pad,
                        end = 1.pad,
                        top = 1.pad,
                        bottom = 1.pad + 80.dp
                    )
                ) {
                    if (events.isNotEmpty()) {
                        item(contentType = -1) {
                            var isInitial by remember {
                                mutableStateOf(true)
                            }
                            LoadMore(true, permanent = true, contentPadding = 1.pad) {
                                if (isInitial) {
                                    isInitial = false
                                } else {
                                    range = (range.first - view.duration) to range.second
                                }
                            }
                        }
                    }

                    var today = shownRange.first
                    while (events.any { it.date >= today }) {
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

                        today = end
                    }

                    if (events.isNotEmpty()) {
                        item(contentType = -1) {
                            LoadMore(true, permanent = true, contentPadding = 1.pad) {
                                range = range.first to (range.second + view.duration)
                            }
                        }
                    }
                }
            }
        }
        AddReminderLayout(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            updates.emit(it)
        }
    }
}
