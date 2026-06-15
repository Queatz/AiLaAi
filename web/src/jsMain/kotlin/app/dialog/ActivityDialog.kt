package app.dialog

import androidx.compose.runtime.*
import Strings
import Styles
import api
import app.ailaai.api.updateCard
import app.reminder.scheduleText
import appString
import application
import com.queatz.db.Activity
import com.queatz.db.Card
import com.queatz.db.Reminder
import components.IconButton
import components.Switch
import kotlinx.coroutines.launch
import lib.addHours
import lib.rawTimeZones
import lib.startOfDay
import lib.systemTimezone
import notEmpty
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date
import kotlin.time.toKotlinInstant

suspend fun configureActivityDialog(
    initial: Activity? = null
): Activity? {
    val isActiveState = mutableStateOf(initial?.active ?: true)
    var resultActivity: Activity? = initial

    val initialTimezone = initial?.timezone?.takeIf { it.isNotBlank() } ?: systemTimezone
    val initialUtcOffset = initial?.utcOffset
        ?: rawTimeZones.firstOrNull { it.name == initialTimezone }?.rawOffsetInMinutes?.toDouble()?.div(60.0)
    val initialStart = initial?.start?.toEpochMilliseconds()?.let { Date(it) }
        ?: initial?.schedule?.hours?.firstOrNull()?.let { hour ->
            addHours(
                date = startOfDay(Date()),
                amount = hour.toDouble()
            )
        }
    val initialEnd = initial?.end?.toEpochMilliseconds()?.let { Date(it) }

    val confirmed = dialog(
        title = application.appString { activity },
        confirmButton = application.appString { confirm },
        cancelButton = application.appString { cancel },
        actions = {
            Switch(
                value = isActiveState.value,
                onValue = {},
                onChange = { isActiveState.value = it },
                border = true,
                title = if (isActiveState.value) appString { Strings.active } else appString { Strings.notActive }
            )
        }
    ) { _ ->
        val scope = rememberCoroutineScope()
        val isActive = isActiveState.value
        var minAge by remember { mutableStateOf(initial?.minAge?.toString() ?: "") }
        var maxAge by remember { mutableStateOf(initial?.maxAge?.toString() ?: "") }
        var minGroupSize by remember { mutableStateOf(initial?.minGroupSize?.toString() ?: "") }
        var maxGroupSize by remember { mutableStateOf(initial?.maxGroupSize?.toString() ?: "") }
        var hasPets by remember { mutableStateOf(initial?.pets ?: false) }
        var isOutdoors by remember { mutableStateOf(initial?.outdoors ?: false) }
        var parking by remember { mutableStateOf(initial?.parking) }
        var languages by remember { mutableStateOf(initial?.languages?.joinToString(", ") ?: "") }
        var duration by remember { mutableStateOf(initial?.duration ?: 0L) }
        var activityStart by remember { mutableStateOf(initialStart) }
        var activityEnd by remember { mutableStateOf(initialEnd) }
        var timezone by remember { mutableStateOf(initialTimezone) }
        var utcOffset by remember { mutableStateOf(initialUtcOffset) }
        var schedule by remember { mutableStateOf(initial?.schedule) }

        LaunchedEffect(
            isActive,
            minAge,
            maxAge,
            minGroupSize,
            maxGroupSize,
            hasPets,
            isOutdoors,
            parking,
            languages,
            duration,
            timezone,
            utcOffset,
            schedule,
            activityStart,
            activityEnd
        ) {
            resultActivity = Activity(
                active = isActive,
                minAge = minAge.toIntOrNull(),
                maxAge = maxAge.toIntOrNull(),
                minGroupSize = minGroupSize.toIntOrNull(),
                maxGroupSize = maxGroupSize.toIntOrNull(),
                pets = hasPets.takeIf { it },
                outdoors = isOutdoors.takeIf { it },
                parking = parking,
                languages = languages.split(",").map { it.trim() }.filter { it.isNotBlank() }.notEmpty,
                duration = duration.takeIf { it > 0 },
                start = activityStart?.toKotlinInstant(),
                end = activityEnd?.toKotlinInstant(),
                timezone = timezone.takeIf { it.isNotBlank() },
                utcOffset = utcOffset,
                schedule = schedule
            )
        }

        ActivityFormContent(
            scope = scope,
            minAge = minAge,
            onMinAgeChange = { minAge = it },
            maxAge = maxAge,
            onMaxAgeChange = { maxAge = it },
            minGroupSize = minGroupSize,
            onMinGroupSizeChange = { minGroupSize = it },
            maxGroupSize = maxGroupSize,
            onMaxGroupSizeChange = { maxGroupSize = it },
            languages = languages,
            onLanguagesChange = { languages = it },
            hasPets = hasPets,
            onHasPetsChange = { hasPets = it },
            isOutdoors = isOutdoors,
            onIsOutdoorsChange = { isOutdoors = it },
            parking = parking,
            onParkingChange = { parking = it },
            duration = duration,
            onDurationChange = { duration = it },
            timezone = timezone,
            onTimezoneChange = { tz, offset ->
                timezone = tz
                utcOffset = offset
            },
            activityStart = activityStart,
            activityEnd = activityEnd,
            schedule = schedule,
            onScheduleChange = { start, end, sched ->
                activityStart = start
                activityEnd = end
                schedule = sched
            }
        )
    }

    return if (confirmed == true) resultActivity else null
}

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
        val isActive = isActiveState.value
        var minAge by remember { mutableStateOf(card.activity?.minAge?.toString() ?: "") }
        var maxAge by remember { mutableStateOf(card.activity?.maxAge?.toString() ?: "") }
        var minGroupSize by remember { mutableStateOf(card.activity?.minGroupSize?.toString() ?: "") }
        var maxGroupSize by remember { mutableStateOf(card.activity?.maxGroupSize?.toString() ?: "") }
        var hasPets by remember { mutableStateOf(card.activity?.pets ?: false) }
        var isOutdoors by remember { mutableStateOf(card.activity?.outdoors ?: false) }
        var parking by remember { mutableStateOf(card.activity?.parking) }
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
            parking,
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
                parking != card.activity?.parking ||
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
                    parking = parking,
                    languages = languages.split(",").map { it.trim() }.filter { it.isNotBlank() }.notEmpty,
                    duration = duration.takeIf { it > 0 },
                    start = activityStart?.toKotlinInstant(),
                    end = activityEnd?.toKotlinInstant(),
                    timezone = timezone.takeIf { it.isNotBlank() },
                    utcOffset = utcOffset,
                    schedule = schedule
                )
                api.updateCard(
                    id = card.id!!,
                    card = Card(activity = updatedActivity)
                ) {
                    onUpdated(it)
                }
            }
        }

        ActivityFormContent(
            scope = scope,
            minAge = minAge,
            onMinAgeChange = { minAge = it },
            maxAge = maxAge,
            onMaxAgeChange = { maxAge = it },
            minGroupSize = minGroupSize,
            onMinGroupSizeChange = { minGroupSize = it },
            maxGroupSize = maxGroupSize,
            onMaxGroupSizeChange = { maxGroupSize = it },
            languages = languages,
            onLanguagesChange = { languages = it },
            hasPets = hasPets,
            onHasPetsChange = { hasPets = it },
            isOutdoors = isOutdoors,
            onIsOutdoorsChange = { isOutdoors = it },
            parking = parking,
            onParkingChange = { parking = it },
            duration = duration,
            onDurationChange = { duration = it },
            timezone = timezone,
            onTimezoneChange = { tz, offset ->
                timezone = tz
                utcOffset = offset
            },
            activityStart = activityStart,
            activityEnd = activityEnd,
            schedule = schedule,
            onScheduleChange = { start, end, sched ->
                activityStart = start
                activityEnd = end
                schedule = sched
            }
        )
    }
}
