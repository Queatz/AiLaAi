package app.dialog

import LocalConfiguration
import androidx.compose.runtime.*
import Strings
import Styles
import api
import app.AppStyles
import app.ailaai.api.updateCard
import app.components.FlexInput
import app.reminder.EditReminderSchedule
import app.reminder.EditSchedule
import app.reminder.formatDuration
import app.reminder.reminderSchedule
import app.reminder.scheduleText
import appString
import application
import com.queatz.db.Activity
import com.queatz.db.Card
import com.queatz.db.Reminder
import components.IconButton
import components.Switch
import focusable
import kotlinx.coroutines.launch
import lib.addHours
import lib.rawTimeZones
import lib.startOfDay
import lib.systemTimezone
import notEmpty
import parseDateTime
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinInstant

suspend fun activityDialog(
    card: Card,
    onUpdated: (Card) -> Unit
) {
    val isActiveState = mutableStateOf(card.activity?.active ?: true)

    dialog(
        title = application.appString { activity },
        confirmButton = application.appString { update },
        cancelButton = null,
        actions = {
            Switch(
                value = isActiveState.value,
                onValue = {},
                onChange = { isActiveState.value = it },
                border = true,
                title = if (isActiveState.value) appString { Strings.active } else appString { Strings.notActive }
            )
        },
        extraButtons = { resolve ->
            if (card.activity != null) {
                val scope = rememberCoroutineScope()
                IconButton(
                    name = "delete",
                    title = appString { remove },
                    onClick = {
                        scope.launch {
                            val result = dialog(
                                title = application.appString { remove },
                                confirmButton = application.appString { delete },
                                cancelButton = application.appString { cancel }
                            ) {
                                Text(application.appString { youCannotUndoThis })
                            }

                            if (result == true) {
                                api.updateCard(
                                    card.id!!,
                                    Card(activity = null)
                                ) {
                                    onUpdated(it)
                                    resolve(true)
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { _ ->
        val scope = rememberCoroutineScope()
        val configuration = LocalConfiguration.current
        val isActive = isActiveState.value
        var minAge by remember { mutableStateOf(card.activity?.minAge?.toString() ?: "") }
        var maxAge by remember { mutableStateOf(card.activity?.maxAge?.toString() ?: "") }
        var minGroupSize by remember { mutableStateOf(card.activity?.minGroupSize?.toString() ?: "") }
        var maxGroupSize by remember { mutableStateOf(card.activity?.maxGroupSize?.toString() ?: "") }
        var hasPets by remember { mutableStateOf(card.activity?.pets ?: false) }
        var isOutdoors by remember { mutableStateOf(card.activity?.outdoors ?: false) }
        var languages by remember { mutableStateOf(card.activity?.languages?.joinToString(", ") ?: "") }
        var duration by remember { mutableStateOf(card.activity?.duration ?: 0L) }
        val initialStart = card.activity?.start?.toEpochMilliseconds()?.let { Date(it) }
            ?: card.activity?.schedule?.hours?.firstOrNull()?.let { hour ->
                addHours(
                    date = startOfDay(Date()),
                    amount = hour.toDouble()
                )
            }
        val initialEnd = card.activity?.end?.toEpochMilliseconds()?.let { Date(it) }
        var activityStart by remember { mutableStateOf(initialStart) }
        var activityEnd by remember { mutableStateOf(initialEnd) }
        val initialTimezone = card.activity?.timezone?.takeIf { it.isNotBlank() } ?: systemTimezone
        val initialUtcOffset = card.activity?.utcOffset
            ?: rawTimeZones.firstOrNull { it.name == initialTimezone }?.rawOffsetInMinutes?.toDouble()?.div(60.0)
        var timezone by remember { mutableStateOf(initialTimezone) }
        var utcOffset by remember { mutableStateOf(initialUtcOffset) }
        var schedule by remember { mutableStateOf(card.activity?.schedule) }

        LaunchedEffect(
            isActive,
            minAge,
            maxAge,
            minGroupSize,
            maxGroupSize,
            hasPets,
            isOutdoors,
            languages,
            duration,
            timezone,
            utcOffset,
            schedule,
            activityStart,
            activityEnd
        ) {
            if (
                isActive != (card.activity?.active ?: true) ||
                minAge != (card.activity?.minAge?.toString() ?: "") ||
                maxAge != (card.activity?.maxAge?.toString() ?: "") ||
                minGroupSize != (card.activity?.minGroupSize?.toString() ?: "") ||
                maxGroupSize != (card.activity?.maxGroupSize?.toString() ?: "") ||
                hasPets != (card.activity?.pets ?: false) ||
                isOutdoors != (card.activity?.outdoors ?: false) ||
                languages != (card.activity?.languages?.joinToString(", ") ?: "") ||
                duration != (card.activity?.duration ?: 0L) ||
                timezone != initialTimezone ||
                utcOffset != initialUtcOffset ||
                schedule != card.activity?.schedule ||
                activityStart != initialStart ||
                activityEnd != initialEnd
            ) {
                val updatedActivity = Activity(
                    active = isActive,
                    minAge = minAge.toIntOrNull(),
                    maxAge = maxAge.toIntOrNull(),
                    minGroupSize = minGroupSize.toIntOrNull(),
                    maxGroupSize = maxGroupSize.toIntOrNull(),
                    pets = hasPets.takeIf { it },
                    outdoors = isOutdoors.takeIf { it },
                    languages = languages.split(",").map { it.trim() }.filter { it.isNotBlank() }.notEmpty,
                    duration = duration.takeIf { it > 0 },
                    start = activityStart?.toKotlinInstant(),
                    end = activityEnd?.toKotlinInstant(),
                    timezone = timezone.takeIf { it.isNotBlank() },
                    utcOffset = utcOffset,
                    schedule = schedule
                )
                api.updateCard(
                    card.id!!,
                    Card(activity = updatedActivity)
                ) {
                    onUpdated(it)
                }
            }
        }

        // Form UI
        Div(
            attrs = {
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                    width(100.percent)
                }
            }
        ) {
            Div(
                attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(0.5.r)
                    }
                }
            ) {
                Text(application.appString { Strings.ageRange })
                Div(
                    attrs = {
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Row)
                            gap(0.5.r)
                        }
                    }
                ) {
                    FlexInput(
                        value = minAge,
                        onChange = { if (it.all { c -> c.isDigit() }) minAge = it },
                        placeholder = application.appString { Strings.minAge },
                        singleLine = true
                    )
                    FlexInput(
                        value = maxAge,
                        onChange = { if (it.all { c -> c.isDigit() }) maxAge = it },
                        placeholder = application.appString { Strings.maxAge },
                        singleLine = true
                    )
                }
            }

            Div(
                attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(0.5.r)
                    }
                }
            ) {
                Text(application.appString { Strings.groupSize })
                Div(
                    attrs = {
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Row)
                            gap(0.5.r)
                        }
                    }
                ) {
                    FlexInput(
                        value = minGroupSize,
                        onChange = { if (it.all { c -> c.isDigit() }) minGroupSize = it },
                        placeholder = application.appString { Strings.minGroupSize },
                        singleLine = true
                    )
                    FlexInput(
                        value = maxGroupSize,
                        onChange = { if (it.all { c -> c.isDigit() }) maxGroupSize = it },
                        placeholder = application.appString { Strings.maxGroupSize },
                        singleLine = true
                    )
                }
            }

            Div(
                attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(0.5.r)
                    }
                }
            ) {
                Text(application.appString { Strings.languages })
                FlexInput(
                    value = languages,
                    onChange = { languages = it },
                    placeholder = application.appString { Strings.languagesPlaceholder },
                    singleLine = true
                )
            }

            Div {
                Input(
                    type = InputType.Checkbox,
                    attrs = {
                        checked(hasPets)
                        onChange { hasPets = it.target.checked }
                    }
                )
                Text(application.appString { Strings.pets })
            }

            Div {
                Input(
                    type = InputType.Checkbox,
                    attrs = {
                        checked(isOutdoors)
                        onChange { isOutdoors = it.target.checked }
                    }
                )
                Text(application.appString { Strings.outdoors })
            }

            Div(
                attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(0.5.r)
                    }
                }
            ) {
                Text(application.appString { Strings.duration })
                Button(
                    attrs = {
                        classes(Styles.outlineButton)
                        style {
                            width(100.percent)
                            justifyContent(JustifyContent.FlexStart)
                        }
                        onClick {
                            scope.launch {
                                var selectedDuration = duration
                                var selectedHours = selectedDuration.milliseconds.inWholeHours.toString()
                                var selectedMinutes = (
                                    selectedDuration.milliseconds.inWholeMinutes -
                                        selectedDuration.milliseconds.inWholeHours * 60L
                                    ).toString()

                                val result = dialog(
                                    title = application.appString { Strings.duration },
                                    confirmButton = application.appString { Strings.confirm }
                                ) {
                                    var hours by remember {
                                        mutableStateOf(
                                            selectedDuration.milliseconds.inWholeHours.toString()
                                        )
                                    }
                                    var minutes by remember {
                                        mutableStateOf(
                                            selectedMinutes
                                        )
                                    }

                                    LaunchedEffect(hours) {
                                        selectedHours = hours
                                    }

                                    LaunchedEffect(minutes) {
                                        selectedMinutes = minutes
                                    }

                                    Div(
                                        attrs = {
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }
                                    ) {
                                        Text(application.appString { Strings.hours })
                                        FlexInput(
                                            value = hours,
                                            onChange = {
                                                val parsedHours = it.toLongOrNull()?.coerceIn(
                                                    minimumValue = 0L,
                                                    maximumValue = 24L
                                                )
                                                hours = parsedHours?.toString() ?: it
                                                if (parsedHours == 24L) {
                                                    minutes = "0"
                                                }
                                            },
                                            placeholder = application.appString { Strings.hours },
                                            singleLine = true,
                                            inputType = InputType.Number,
                                            use100Width = true,
                                            enableVoiceInput = false
                                        )
                                    }

                                    Div(
                                        attrs = {
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }
                                    ) {
                                        Text(application.appString { Strings.minutes })
                                        FlexInput(
                                            value = minutes,
                                            onChange = {
                                                val parsedHours = hours.toLongOrNull()?.coerceIn(
                                                    minimumValue = 0L,
                                                    maximumValue = 24L
                                                )
                                                if (parsedHours == 24L) {
                                                    minutes = "0"
                                                } else {
                                                    val parsedMinutes = it.toLongOrNull()?.coerceIn(
                                                        minimumValue = 0L,
                                                        maximumValue = 59L
                                                    )
                                                    minutes = parsedMinutes?.toString() ?: it
                                                }
                                            },
                                            placeholder = application.appString { Strings.minutes },
                                            singleLine = true,
                                            inputType = InputType.Number,
                                            use100Width = true,
                                            enableVoiceInput = false
                                        )
                                    }
                                }

                                if (result == true) {
                                    val parsedHours = (selectedHours.toLongOrNull() ?: 0L).coerceIn(
                                        minimumValue = 0L,
                                        maximumValue = 24L
                                    )
                                    val parsedMinutes = if (parsedHours == 24L) {
                                        0L
                                    } else {
                                        (selectedMinutes.toLongOrNull() ?: 0L).coerceIn(
                                            minimumValue = 0L,
                                            maximumValue = 59L
                                        )
                                    }

                                    selectedDuration =
                                        parsedHours * 60L * 60L * 1000L +
                                            parsedMinutes * 60L * 1000L

                                    duration = selectedDuration
                                }
                            }
                        }
                    }
                ) {
                    Text(duration.formatDuration() ?: application.appString { Strings.duration })
                }
            }

            Div(
                attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(0.5.r)
                    }
                }
            ) {
                Text(application.appString { Strings.timezone })
                Button(
                    attrs = {
                        classes(Styles.outlineButton)
                        style {
                            width(100.percent)
                            justifyContent(JustifyContent.FlexStart)
                        }
                        onClick {
                            scope.launch {
                                searchDialog(
                                    configuration = configuration,
                                    title = application.appString { Strings.timezone },
                                    defaultValue = timezone.replace("_", " "),
                                    load = {
                                        rawTimeZones.toList()
                                    },
                                    filter = { value, search ->
                                        val hours = value.rawOffsetInMinutes.toDouble() / 60.0
                                        "${value.name.replace("_", " ")} ${if (hours < 0) "" else "+"}$hours".contains(search, true)
                                    }
                                ) { selectedTimezone, resolve ->
                                    Div({
                                        classes(AppStyles.groupItem)

                                        onClick {
                                            timezone = selectedTimezone.name
                                            utcOffset = selectedTimezone.rawOffsetInMinutes.toDouble() / 60.0
                                            resolve(true)
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
                                                Text(selectedTimezone.name.replace("_", " "))
                                            }
                                            Div({
                                                classes(AppStyles.groupItemMessage)
                                            }) {
                                                val hours = selectedTimezone.rawOffsetInMinutes.toDouble() / 60.0
                                                Text("UTC ${if (hours < 0) "" else "+"}$hours")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(timezone.replace("_", " ").ifBlank { application.appString { Strings.timezone } })
                }
            }

            Div(
                attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(0.5.r)
                    }
                }
            ) {
                Text(application.appString { Strings.schedule })
                Button(
                    attrs = {
                        classes(Styles.outlineButton)
                        style {
                            width(100.percent)
                            justifyContent(JustifyContent.FlexStart)
                        }
                        onClick {
                            scope.launch {
                                val initialScheduleDate = activityStart
                                    ?: schedule?.hours?.firstOrNull()?.let { hour ->
                                        addHours(
                                            date = startOfDay(Date()),
                                            amount = hour.toDouble()
                                        )
                                    }
                                    ?: startOfDay(Date())

                                val editSchedule = EditSchedule(
                                    initialReoccurs = schedule != null,
                                    initialUntil = activityEnd != null,
                                    initialDate = initialScheduleDate,
                                    initialUntilDate = activityEnd,
                                    initialReoccurringHours = schedule?.hours,
                                    initialReoccurringDays = schedule?.days,
                                    initialReoccurringWeekdays = schedule?.weekdays,
                                    initialReoccurringWeeks = schedule?.weeks,
                                    initialReoccurringMonths = schedule?.months
                                )

                                val result = dialog(
                                    title = application.appString { Strings.schedule },
                                    confirmButton = application.appString { Strings.update }
                                ) {
                                    EditReminderSchedule(
                                        schedule = editSchedule,
                                        includeStickiness = false
                                    )
                                }

                                if (result == true) {
                                    val selectedScheduleStart = parseDateTime(
                                        dateStr = editSchedule.date,
                                        timeStr = editSchedule.time
                                    )
                                    val selectedScheduleEnd = if (editSchedule.until) {
                                        parseDateTime(
                                            dateStr = editSchedule.untilDate,
                                            timeStr = editSchedule.untilTime
                                        )
                                    } else {
                                        null
                                    }
                                    activityStart = selectedScheduleStart
                                    activityEnd = selectedScheduleEnd
                                    val selectedHour = selectedScheduleStart.getHours().coerceIn(0, 23)
                                    schedule = editSchedule.reminderSchedule?.let { reminderSchedule ->
                                        reminderSchedule.copy(
                                            hours = listOf(selectedHour)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        if (schedule == null) {
                            application.appString { Strings.schedule }
                        } else {
                            val summaryStart = activityStart
                                ?: schedule?.hours?.firstOrNull()?.let { hour ->
                                    addHours(
                                        date = startOfDay(Date()),
                                        amount = hour.toDouble()
                                    )
                                }
                                ?: startOfDay(Date())
                            Reminder(
                                start = summaryStart.toKotlinInstant(),
                                schedule = schedule
                            ).scheduleText
                        }
                    )
                }
            }
        }
    }
}
