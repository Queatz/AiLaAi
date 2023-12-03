package app.nav

import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.newReminder
import app.ailaai.api.reminders
import app.components.Spacer
import app.page.ScheduleView
import app.reminder.*
import appString
import appText
import application
import com.queatz.db.Reminder
import components.IconButton
import components.Loading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import lib.getTimezoneOffset
import lib.systemTimezone
import opensavvy.compose.lazy.LazyColumn
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.events.Event
import r

@Composable
fun ScheduleNavPage(
    reminderUpdates: Flow<Reminder>,
    reminder: Reminder?,
    onReminder: (Reminder?) -> Unit,
    onUpdate: (Reminder) -> Unit,
    view: ScheduleView,
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
                    utcOffset = getTimezoneOffset(timezone) / (60.0 * 60.0 * 1000.0),
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
        IconButton("search", appString { search }, styles = {
            marginRight(.5.r)
        }) {
            showSearch = !showSearch
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
                    it.target.style.height = "0"
                    it.target.style.height = "${it.target.scrollHeight + 2}px"
                }

                onChange {
                    it.target.style.height = "0"
                    it.target.style.height = "${it.target.scrollHeight + 2}px"

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
            key(shownReminders, reminder) { // todo remove after LazyColumn library is updated
                LazyColumn {
                    items(shownReminders) {
                        ReminderItem(it, selected = reminder?.id == it.id) {
                            onReminder(it)
                        }
                    }
                }
            }
        }
    }
}
