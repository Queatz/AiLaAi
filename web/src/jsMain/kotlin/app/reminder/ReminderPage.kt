package app.reminder

import LocalConfiguration
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
import app.AppStyles
import app.PageTopBar
import app.ailaai.api.deleteReminder
import app.ailaai.api.groups
import app.ailaai.api.people
import app.ailaai.api.profile
import app.ailaai.api.updateReminder
import app.dialog.photoDialog
import baseUrl
import components.CardContent
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.css.percent
import stories.StoryStyles
import stories.StoryContents
import app.components.EditField
import app.dialog.dialog
import app.dialog.inputDialog
import app.dialog.searchDialog
import app.group.friendsDialog
import app.menu.Menu
import appString
import appText
import application
import components.ProfilePhoto
import components.Switch
import bulletedString
import com.queatz.db.Card
import com.queatz.db.Person
import com.queatz.db.Reminder
import focusable
import kotlinx.coroutines.launch
import lib.RawTimeZone
import lib.rawTimeZones
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.NumberInput
import org.jetbrains.compose.web.dom.Text
import kotlinx.browser.window
import notBlank
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import stories.asStoryContents
import kotlin.js.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Composable
fun ReminderPage(
    reminder: Reminder,
    onUpdate: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    val scope = rememberCoroutineScope()
    val me = application.me.collectAsState().value

    var menuTarget by remember(reminder) {
        mutableStateOf<DOMRect?>(null)
    }

    val schedule by remember(reminder) {
        mutableStateOf(
            EditSchedule(
                initialReoccurs = reminder.schedule != null,
                initialUntil = reminder.end != null,
                initialDate = Date(reminder.start!!.toEpochMilliseconds()),
                initialUntilDate = reminder.end?.let { Date(it.toEpochMilliseconds()) },
                initialReoccurringHours = reminder.schedule?.hours,
                initialReoccurringDays = reminder.schedule?.days,
                initialReoccurringWeekdays = reminder.schedule?.weekdays,
                initialReoccurringWeeks = reminder.schedule?.weeks,
                initialReoccurringMonths = reminder.schedule?.months,
                initialStickiness = reminder.stickiness
            )
        )
    }

    menuTarget?.let { target ->
        Menu({ menuTarget = null }, target) {
            item(appString { rename }) {
                scope.launch {
                    val title = inputDialog(
                        title = application.appString { this.reminder },
                        placeholder = application.appString { title },
                        confirmButton = application.appString { update },
                        defaultValue = reminder.title ?: ""
                    )

                    if (title == null) return@launch

                    api.updateReminder(
                        id = reminder.id!!,
                        reminder = Reminder(title = title)
                    ) {
                        onUpdate(it)
                    }
                }
            }

            item(appString { reschedule }) {
                scope.launch {
                    val result = dialog(
                        title = application.appString { reschedule },
                        confirmButton = application.appString { update }
                    ) {
                        EditReminderSchedule(schedule)
                    }

                    if (result == true) {
                        api.updateReminder(
                            id = reminder.id!!,
                            reminder = Reminder(
                                start = schedule.start,
                                end = schedule.end,
                                schedule = schedule.reminderSchedule,
                                stickiness = schedule.stickiness.takeIf { schedule.hasStickiness }
                            )
                        ) {
                            onUpdate(it)
                        }
                    }
                }
            }

            item(
                if (reminder.alarm == true) {
                    // todo: translate
                    "Turn off alarm"
                } else {
                    // todo: translate
                    "Turn on alarm"
                },
                icon = "alarm".takeIf { reminder.alarm == true }
            ) {
                scope.launch {
                    api.updateReminder(
                        id = reminder.id!!,
                        reminder = Reminder(
                            alarm = reminder.alarm != true
                        )
                    ) {
                        onUpdate(it)
                    }
                }
            }

            val configuration = LocalConfiguration.current

            suspend fun setTimezone(timezone: RawTimeZone) {
                api.updateReminder(
                    id = reminder.id!!,
                    reminder = Reminder(
                        timezone = timezone.name,
                        utcOffset = timezone.rawOffsetInMinutes.toDouble() / 60.0
                    )
                ) {
                    onUpdate(it)
                }
            }

            val durationString = appString { duration }

            item(durationString) {
                scope.launch {
                    var duration = reminder.duration ?: 0L

                    val result = dialog(
                        title = durationString,
                        confirmButton = application.appString { confirm }
                    ) {
                        var hours by remember {
                            mutableStateOf(
                                duration.milliseconds.inWholeHours
                            )
                        }
                        var minutes by remember {
                            mutableStateOf(
                                duration.milliseconds.inWholeMinutes - hours.hours.inWholeMinutes
                            )
                        }

                        LaunchedEffect(hours, minutes) {
                            duration = hours.hours.inWholeMilliseconds + minutes.minutes.inWholeMilliseconds

                            if (hours == 24L) {
                                minutes = 0L
                            }
                        }

                        H3 {
                            appText { this.hours }
                        }
                        NumberInput(
                            value = hours,
                            min = 0,
                            max = 24,
                            attrs = {
                                classes(Styles.dateTimeInput)

                                style {
                                    padding(1.r)
                                }

                                onInput {
                                    runCatching {
                                        hours = (it.value?.toLong() ?: 0L).coerceIn(0L..24L)
                                    }
                                }
                            }
                        )
                        H3 {
                            appText { this.minutes }
                        }
                        NumberInput(
                            value = minutes,
                            min = 0,
                            max = 59,
                            attrs = {
                                classes(Styles.dateTimeInput)
                                style {
                                    padding(1.r)
                                }

                                onInput {
                                    minutes = (it.value?.toLong() ?: 0L).coerceIn(0L..59L)
                                }
                            }
                        )
                    }

                    if (result != null) {
                        api.updateReminder(
                            id = reminder.id!!,
                            reminder = Reminder(duration = duration),
                        ) {
                            onUpdate(it)
                        }
                    }
                }
            }

            item(appString { timezone }) {
                scope.launch {
                    searchDialog(
                        configuration = configuration,
                        title = application.appString { timezone },
                        defaultValue = reminder.timezone?.replace("_", " ") ?: "",
                        load = {
                            rawTimeZones.toList()
                        },
                        filter = { it, search ->
                            val hours = it.rawOffsetInMinutes.toDouble() / 60.0
                            "${it.name.replace("_", " ")} ${if (hours < 0) "" else "+"}$hours".contains(search, true)
                        }
                    ) { timezone, resolve ->
                        Div({
                            classes(AppStyles.groupItem)

                            onClick {
                                scope.launch {
                                    setTimezone(timezone)
                                    resolve(true)
                                }
                            }

                            focusable()
                        }) {
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                }
                            }) {
                                Div({
                                    classes(AppStyles.groupItemName)
                                }) {
                                    Text(timezone.name.replace("_", " "))
                                }
                                Div({
                                    classes(AppStyles.groupItemMessage)
                                }) {
                                    val hours = timezone.rawOffsetInMinutes.toDouble() / 60.0
                                    Text("UTC ${if (hours < 0) "" else "+"}$hours")
                                }
                            }
                        }
                    }
                }
            }

            item(appString { invite }) {
                scope.launch {
                    friendsDialog(
                        omit = (reminder.people ?: emptyList()),
                        actions = { _ ->
                            // No additional actions needed
                        }
                    ) { selectedPeople ->
                        if (selectedPeople.isNotEmpty()) {
                            scope.launch {
                                api.updateReminder(
                                    id = reminder.id!!,
                                    reminder = Reminder(
                                        people = ((reminder.people ?: emptyList()) + selectedPeople.map { it.id!! }).distinct()
                                    )
                                ) {
                                    onUpdate(it)
                                }
                            }
                        }
                    }
                }
            }

            item(appString { delete }) {
                scope.launch {
                    // Todo: translate
                    val result = dialog(
                        title = "Delete this reminder?",
                        confirmButton = application.appString { yesDelete }
                    ) {
                        appText { youCannotUndoThis }
                    }

                    if (result != true) return@launch

                    api.deleteReminder(reminder.id!!) {
                        onDelete(reminder)
                    }
                }
            }
        }
    }

    Div({
        style {
            flex(1)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            overflowY("auto")
            overflowX("hidden")
        }
    }) {
        EditField(
            value = reminder.note ?: "",
            placeholder = appString { note },
            styles = {
                margin(1.r, 1.r, 0.r, 1.r)
            }
        ) {
            var success = false
            api.updateReminder(reminder.id!!, Reminder(note = it)) {
                success = true
                onUpdate(it)
            }

            success
        }

        // Render people on the reminder if there are any
        val people = (reminder.people.orEmpty() + reminder.person!!).takeIf {
            !(it.size == 1 && it.first() == me?.id)
        }
        if (!people.isNullOrEmpty()) {
            // Add a title for the people section
            Div({
                style {
                    margin(1.r, 1.r, 0.r, 1.r)
                    fontWeight("bold")
                }
            }) {
                appText { this.people }
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Row)
                    flexWrap(FlexWrap.Wrap)
                    margin(1.r)
                }
            }) {
                people.forEach { personId ->
                    // Load person data and render ProfilePhoto
                    var person by remember(personId) {
                        mutableStateOf<Person?>(null)
                    }

                    LaunchedEffect(personId) {
                        api.profile(personId) {
                            person = it.person
                        }
                    }

                    person?.let { p ->
                        ProfilePhoto(
                            person = p,
                            size = 48.px,
                            border = true,
                            onClick = {
                                window.open("/profile/${p.id}", "_blank")
                            },
                            styles = {
                                margin(0.r, .5.r, 0.r, 0.r)
                            }
                        )
                    }
                }
            }
        }

        // Display reminder photo if available
        reminder.photo?.let { photo ->
            Div({
                style {
                    margin(1.r)
                }
            }) {
                val url = "$baseUrl$photo"
                Img(src = url, attrs = {
                    classes(StoryStyles.contentPhotosPhoto)

                    onClick {
                        scope.launch {
                            photoDialog(url)
                        }
                    }
                })
            }
        }

        val content = remember(reminder.content) {
            reminder.content?.notBlank?.asStoryContents() ?: emptyList()
        }

        // Display reminder content if available
        if (content.isNotEmpty()) {
            Div({
                style {
                    marginLeft(1.r)
                    marginRight(1.r)
                }
            }) {
                StoryContents(
                    content = content
                )
            }
        }

        ReminderEvents(reminder)
    }
    var isOpen by remember(reminder) {
        mutableStateOf(reminder.open == true)
    }

    PageTopBar(
        title = reminder.title ?: appString { newGroup },
        description = bulletedString(
            reminder.categories?.firstOrNull(),
            reminder.scheduleText
        ),
        actions = {
            Switch(
                value = isOpen,
                onValue = { isOpen = it },
                onChange = {
                    scope.launch {
                        val previousValue = reminder.open == true
                        api.updateReminder(
                            id = reminder.id!!,
                            reminder = Reminder(open = it),
                            onError = {
                                isOpen = previousValue
                            }
                        ) {
                            onUpdate(it)
                        }
                    }
                },
                title = if (isOpen) {
                    application.appString { reminderIsOpen }
                } else {
                    application.appString { reminderIsClosed }
                }
            ) {
                margin(1.r)
            }
        }
    ) {
        menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
    }
}
