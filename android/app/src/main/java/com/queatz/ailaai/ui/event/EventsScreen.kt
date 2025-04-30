package com.queatz.ailaai.ui.event

import ReminderEvent
import ReminderEventType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.newReminder
import app.ailaai.api.occurrences
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.formatDateForToday
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.sortedDistinct
import com.queatz.ailaai.extensions.startOfMinute
import com.queatz.ailaai.helpers.LocationSelector
import com.queatz.ailaai.nav
import com.queatz.ailaai.schedule.ScheduleReminderDialog
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.FloatingButton
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.LoadMore
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.screens.SearchContent
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Geo
import com.queatz.db.Reminder
import com.queatz.db.toList
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import toEvents

@Composable
fun EventsScreen(
    geo: Geo?,
    locationSelector: LocationSelector,
    header: LazyListScope.() -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(true)
    var events by rememberStateOf(emptyList<ReminderEvent>())
    var endDate by rememberStateOf(Clock.System.now().plus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault()))
    val nav = nav
    var h by rememberStateOf(80.dp.px)
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = remember(events) {
        events
            .flatMap { it.reminder.categories ?: emptyList() }
            .sortedDistinct()
    }

    suspend fun loadEvents(loadMore: Boolean = false) {
        val start = Clock.System.now()
        val end = endDate

        if (!loadMore) {
            isLoading = true
        }

        api.occurrences(
            start = start,
            end = end,
            open = true,
            geo = geo?.toList()
        ) { occurrences ->
            val newEvents = occurrences.toEvents()
                .filter { it.event != ReminderEventType.End }
                .sortedBy { it.date }

            events = if (loadMore) {
                (events + newEvents)
                    .distinctBy { it.reminder.id to it.date }
                    .sortedBy { it.date }
            } else {
                newEvents
            }
            isLoading = false
        }
    }

    fun reload() {
        scope.launch {
            loadEvents()
        }
    }

    val shownEvents = events.let { events ->
        // Filter by search text
        if (searchText.isNotBlank()) {
            events.filter {
                it.reminder.title?.contains(searchText, ignoreCase = true) == true ||
                        it.reminder.note?.contains(searchText, ignoreCase = true) == true ||
                        it.reminder.content?.contains(searchText, ignoreCase = true) == true
            }
        } else {
            events
        }
    }.let { events ->
        // Filter by category
        if (selectedCategory != null) {
            events.filter { it.reminder.categories?.contains(selectedCategory) == true }
        } else {
            events
        }
    }

    LaunchedEffect(geo, searchText, selectedCategory) {
        loadEvents()
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
    ) {
        val groupedEvents = shownEvents.groupBy {
            val localDate = it.date.toLocalDateTime(TimeZone.currentSystemDefault()).date
            localDate
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 1.pad,
                top = 0.dp,
                end = 1.pad,
                bottom = 3.5f.pad + h.inDp()
            ),
            verticalArrangement = Arrangement.spacedBy(1.pad),
            modifier = Modifier
                .fillMaxSize()
        ) {
            header(this)

            if (isLoading && events.isEmpty()) {
                item {
                    Loading(
                        modifier = Modifier
                            .padding(2.pad)
                    )
                }
            } else if (groupedEvents.isEmpty()) {
                item {
                    EmptyText(stringResource(R.string.none))
                }
            } else {
                groupedEvents.forEach { (date, dateEvents) ->
                    item {
                        Text(
                            text = date.atTime(0, 0).toInstant(TimeZone.currentSystemDefault()).formatDateForToday(),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 1.pad, vertical = 0.5f.pad)
                        )
                    }

                    if (dateEvents.isEmpty()) {
                        item {
                            EmptyText(stringResource(R.string.no_events))
                        }
                    } else {
                        items(dateEvents) { event ->
                            EventItem(
                                event = event,
                                onUpdated = {
                                    reload()
                                },
                                onClick = {
                                    nav.appNavigate(AppNav.Reminder(event.reminder.id!!))
                                }
                            )
                        }
                    }
                }

                item {
                    LoadMore(
                        hasMore = true,
                        permanent = true,
                        visible = false,
                        contentPadding = 1.pad
                    ) {
                        if (!isLoading) {
                            endDate = endDate.plus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                            scope.launch {
                                loadEvents(true)
                            }
                        }
                    }
                }
            }
        }
        PageInput(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onPlaced {
                    h = it.size.height
                }
        ) {
            SearchContent(
                locationSelector = locationSelector,
                isLoading = isLoading,
                categories = categories,
                category = selectedCategory
            ) {
                selectedCategory = it
            }
            SearchFieldAndAction(
                value = searchText,
                valueChange = { searchText = it },
                placeholder = stringResource(R.string.search),
                action = {
                    var showScheduleReminder by remember { mutableStateOf(false) }

                    FloatingButton(
                        onClick = {
                            showScheduleReminder = true
                        },
                        onLongClick = {},
                        onClickLabel = stringResource(R.string.create_event)
                    ) {
                        Icon(Icons.Outlined.Add, stringResource(R.string.create_event))
                    }

                    if (showScheduleReminder) {
                        ScheduleReminderDialog(
                            onDismissRequest = {
                                showScheduleReminder = false
                            },
                            initialReminder = Reminder(
                                start = Clock.System.now().startOfMinute(),
                                open = true,
                                geo = geo?.toList()
                            ),
                            showTitle = true,
                            confirmText = stringResource(R.string.create_event)
                        ) { reminder ->
                            scope.launch {
                                api.newReminder(
                                    Reminder(
                                        title = reminder.title,
                                        start = reminder.start,
                                        end = reminder.end,
                                        schedule = reminder.schedule,
                                        timezone = TimeZone.currentSystemDefault().id,
                                        utcOffset = TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds / (60.0 * 60.0),
                                        open = true,
                                        geo = geo?.toList()
                                    )
                                ) { newReminder ->
                                    loadEvents(false)
                                    nav.appNavigate(AppNav.Reminder(newReminder.id!!))
                                }
                            }
                            showScheduleReminder = false
                        }
                    }
                }
            )
        }
    }
}
