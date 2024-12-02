package app.nav

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.newReminder
import app.ailaai.api.reminders
import app.components.Spacer
import app.page.ScheduleView
import app.page.ScheduleViewType
import app.reminder.EditReminderSchedule
import app.reminder.EditSchedule
import app.reminder.ReminderItem
import app.reminder.end
import app.reminder.reminderSchedule
import app.reminder.start
import appString
import appText
import application
import com.queatz.db.Reminder
import components.IconButton
import components.Loading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import lib.rawTimeZones
import lib.systemTimezone
import opensavvy.compose.lazy.LazyColumn
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.div
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.events.Event
import r
import resize

@Composable
fun ScheduleNavPage(
    reminderUpdates: Flow<Reminder>,
    reminder: Reminder?,
    onReminder: (Reminder?) -> Unit,
    onUpdate: (Reminder) -> Unit,
    view: ScheduleView,
    viewType: ScheduleViewType,
    onViewTypeClick: (ScheduleViewType) -> Unit,
    onViewClick: (ScheduleView) -> Unit,
    onProfileClick: () -> Unit
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()

    var newReminderTitle by remember {
        mutableStateOf("")
    }

    var isSavingReminder by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var reminders by remember {
        mutableStateOf(emptyList<Reminder>())
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    LaunchedEffect(reminder, view) {
        searchText = ""
        showSearch = false
    }

    val shownReminders = remember(reminders, searchText) {
        val search = searchText.trim()
        if (searchText.isBlank()) {
            reminders
        } else {
            reminders.filter {
                it.title?.contains(search, true) ?: false
            }
        }
    }

    var onValueChange by remember { mutableStateOf({}) }

    val schedule by remember(newReminderTitle == "") {
        mutableStateOf(EditSchedule())
    }

    LaunchedEffect(newReminderTitle) {
        onValueChange()
    }

    fun reload(select: Boolean = true) {
        scope.launch {
            api.reminders {
                reminders = it

                if (select && reminder != null) {
                    onReminder(reminders.firstOrNull { it.id == reminder.id })
                }
            }
            isLoading = false
        }
    }

    fun addReminder(open: Boolean = false) {
        val timezone = systemTimezone

        if (newReminderTitle.isBlank()) {
            return
        }

        scope.launch {
            isSavingReminder = true

            api.newReminder(
                Reminder(
                    title = newReminderTitle,
                    start = schedule.start,
                    end = schedule.end,
                    timezone = timezone,
                    utcOffset = rawTimeZones.toList().firstOrNull { it.name == timezone }?.rawOffsetInMinutes?.toDouble()?.div(60.0) ?: 0.0,
                    schedule = schedule.reminderSchedule
                )
            ) {
                newReminderTitle = ""
                reload(false)
                if (open) {
                    onReminder(it)
                } else {
                    onUpdate(it)
                }
            }

            isSavingReminder = false
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    LaunchedEffect(reminder) {
        reminderUpdates.collectLatest {
            reload()
        }
    }

    NavTopBar(me, appString { this.reminders }, onProfileClick) {
        IconButton("search", appString { search }) {
            showSearch = !showSearch
        }
        // todo: translate
        IconButton(if (viewType == ScheduleViewType.Schedule) "view_agenda" else "view_week", "Switch view", styles = {
            marginRight(.5.r)
        }) {
            onViewTypeClick(
                when (viewType) {
                    ScheduleViewType.Schedule -> ScheduleViewType.Calendar
                    else -> ScheduleViewType.Schedule
                }
            )
        }
    }

    if (showSearch) {
        NavSearchInput(searchText, { searchText = it }, onDismissRequest = {
            searchText = ""
            showSearch = false
        })
    }

    Div({
        style {
            overflowY("auto")
            overflowX("hidden")
            property("scrollbar-width", "none")
            padding(1.r / 2)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        val createReminder = appString { createReminder }

        if (!showSearch) {
            // todo can be EditField
            TextArea(newReminderTitle) {
                classes(Styles.textarea)
                style {
                    margin(0.r, .5.r, .5.r, .5.r)
                    height(3.5.r)
                    maxHeight(6.5.r)
                }

                onKeyDown {
                    if (it.key == "Enter" && !it.shiftKey) {
                        it.preventDefault()
                        it.stopPropagation()
                        addReminder(it.ctrlKey)
                    }
                }

                onInput {
                    newReminderTitle = it.value
                    it.target.resize()
                }

                onChange {
                    it.target.resize()

                    if (newReminderTitle.isEmpty()) {
                        it.target.focus()
                    }
                }

                if (isSavingReminder) {
                    disabled()
                }

                placeholder(createReminder)

                autoFocus()

                ref { element ->
                    element.focus()

                    onValueChange = { element.dispatchEvent(Event("change")) }

                    onDispose {
                        onValueChange = {}
                    }
                }
            }

            if (newReminderTitle.isNotBlank()) {
                EditReminderSchedule(schedule)

                Button({
                    classes(Styles.button)

                    style {
                        padding(.5.r, 1.r)
                        margin(0.r, .5.r, 1.r, .5.r)
                        justifyContent(JustifyContent.Center)
                        flexShrink(0)
                        fontWeight("bold")
                    }

                    onClick {
                        addReminder(it.ctrlKey)
                    }

                    if (isSavingReminder) {
                        disabled()
                    }
                }) {
                    appText { this.createReminder }
                }
            }

            if (newReminderTitle.isNotBlank()) {
                return@Div
            }

            // todo this is same as groupsnavpage Should be NavMainContent
            Div({
            }) {
                NavMenuItem(
                    "routine",
                    appString { daily },
                    selected = reminder == null && view == ScheduleView.Daily
                )
                { onViewClick(ScheduleView.Daily) }
                NavMenuItem(
                    "calendar_view_week",
                    appString { weekly },
                    selected = reminder == null && view == ScheduleView.Weekly
                ) { onViewClick(ScheduleView.Weekly) }
                NavMenuItem(
                    "calendar_month",
                    appString { monthly },
                    selected = reminder == null && view == ScheduleView.Monthly
                ) { onViewClick(ScheduleView.Monthly) }
                NavMenuItem(
                    "auto_mode",
                    appString { yearly },
                    selected = reminder == null && view == ScheduleView.Yearly
                ) { onViewClick(ScheduleView.Yearly) }
            }
            Spacer()
        }

        if (isLoading) {
            Loading()
        } else {
            LazyColumn {
                items(shownReminders, key = { it.id!! }) {
                    ReminderItem(reminder = it, selected = reminder?.id == it.id) {
                        onReminder(it)
                    }
                }
            }
        }
    }
}
