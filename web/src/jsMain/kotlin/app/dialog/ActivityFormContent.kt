package app.dialog

import LocalConfiguration
import Strings
import Styles
import app.AppStyles
import app.components.FlexInput
import app.reminder.EditReminderSchedule
import app.reminder.EditSchedule
import app.reminder.formatDuration
import app.reminder.reminderSchedule
import app.reminder.scheduleText
import appString
import application
import com.queatz.db.Parking
import com.queatz.db.Reminder
import com.queatz.db.ReminderSchedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import focusable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lib.addHours
import lib.rawTimeZones
import lib.startOfDay
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import parseDateTime
import r
import kotlin.js.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinInstant

@Composable
fun ActivityFormContent(
    scope: CoroutineScope,
    minAge: String,
    onMinAgeChange: (String) -> Unit,
    maxAge: String,
    onMaxAgeChange: (String) -> Unit,
    minGroupSize: String,
    onMinGroupSizeChange: (String) -> Unit,
    maxGroupSize: String,
    onMaxGroupSizeChange: (String) -> Unit,
    languages: String,
    onLanguagesChange: (String) -> Unit,
    hasPets: Boolean,
    onHasPetsChange: (Boolean) -> Unit,
    isOutdoors: Boolean,
    onIsOutdoorsChange: (Boolean) -> Unit,
    parking: Parking?,
    onParkingChange: (Parking?) -> Unit,
    duration: Long,
    onDurationChange: (Long) -> Unit,
    timezone: String,
    onTimezoneChange: (String, Double) -> Unit,
    activityStart: Date?,
    activityEnd: Date?,
    schedule: ReminderSchedule?,
    onScheduleChange: (Date?, Date?, ReminderSchedule?) -> Unit,
) {
    val configuration = LocalConfiguration.current

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
                        width(100.percent)
                    }
                }
            ) {
                FlexInput(
                    value = minAge,
                    onChange = { if (it.all { c -> c.isDigit() }) onMinAgeChange(it) },
                    placeholder = application.appString { Strings.minAge },
                    singleLine = true,
                    use100Width = true,
                    useDefaultWidth = false
                )
                FlexInput(
                    value = maxAge,
                    onChange = { if (it.all { c -> c.isDigit() }) onMaxAgeChange(it) },
                    placeholder = application.appString { Strings.maxAge },
                    singleLine = true,
                    use100Width = true,
                    useDefaultWidth = false
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
                        width(100.percent)
                    }
                }
            ) {
                FlexInput(
                    value = minGroupSize,
                    onChange = { if (it.all { c -> c.isDigit() }) onMinGroupSizeChange(it) },
                    placeholder = application.appString { Strings.minGroupSize },
                    singleLine = true,
                    use100Width = true,
                    useDefaultWidth = false
                )
                FlexInput(
                    value = maxGroupSize,
                    onChange = { if (it.all { c -> c.isDigit() }) onMaxGroupSizeChange(it) },
                    placeholder = application.appString { Strings.maxGroupSize },
                    singleLine = true,
                    use100Width = true,
                    useDefaultWidth = false
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
                onChange = { onLanguagesChange(it) },
                placeholder = application.appString { Strings.languagesPlaceholder },
                singleLine = true
            )
        }

        Div {
            Input(
                type = InputType.Checkbox,
                attrs = {
                    checked(hasPets)
                    onChange { onHasPetsChange(it.target.checked) }
                }
            )
            Text(application.appString { Strings.pets })
        }

        Div {
            Input(
                type = InputType.Checkbox,
                attrs = {
                    checked(isOutdoors)
                    onChange { onIsOutdoorsChange(it.target.checked) }
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
            Text(application.appString { Strings.parking })
            Div(
                attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        gap(0.5.r)
                        flexWrap(FlexWrap.Wrap)
                    }
                }
            ) {
                listOf(
                    Parking.None to application.appString { Strings.parkingNone },
                    Parking.Bike to application.appString { Strings.parkingBike },
                    Parking.Motorbike to application.appString { Strings.parkingMotorbike },
                    Parking.Car to application.appString { Strings.parkingCar }
                ).forEach { (option, label) ->
                    Button(
                        attrs = {
                            classes(
                                if (parking == option) Styles.button else Styles.outlineButton
                            )
                            onClick { onParkingChange(if (parking == option) null else option) }
                        }
                    ) {
                        Text(label)
                    }
                }
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

                                onDurationChange(selectedDuration)
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
                                        onTimezoneChange(
                                            selectedTimezone.name,
                                            selectedTimezone.rawOffsetInMinutes.toDouble() / 60.0
                                        )
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
                                val selectedHour = selectedScheduleStart.getHours().coerceIn(0, 23)
                                val newSchedule = editSchedule.reminderSchedule?.copy(
                                    hours = listOf(selectedHour)
                                )
                                onScheduleChange(selectedScheduleStart, selectedScheduleEnd, newSchedule)
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
                            ?: schedule.hours?.firstOrNull()?.let { hour ->
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
