package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Activity
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Composable
fun CardActivity(activity: Activity) {
    val schedule = activity.schedule
    if (schedule != null) {
        val isAvailable = isAvailableToday(activity)
        if (isAvailable) {
            ActivityItem(
                Icons.Outlined.CalendarToday,
                formatSchedule(activity) + " " + stringResource(R.string.today_inline)
            )
        } else {
            ActivityItem(
                Icons.Outlined.CalendarToday,
                stringResource(R.string.not_available_today),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
    activity.duration?.let {
        ActivityItem(
            Icons.Outlined.Schedule,
            stringResource(R.string.duration_minutes).format((it / 1000 / 60).toString())
        )
    }
    val minAge = activity.minAge
    val maxAge = activity.maxAge
    if (minAge != null || maxAge != null) {
        val text = when {
            minAge != null && maxAge != null -> stringResource(R.string.age_range_value).format(
                minAge.toString(),
                maxAge.toString()
            )

            minAge != null -> stringResource(R.string.age_min_value).format(minAge.toString())
            else -> stringResource(R.string.age_max_value).format(maxAge.toString())
        }
        ActivityItem(Icons.Outlined.Face, text)
    }
    val minGroup = activity.minGroupSize
    val maxGroup = activity.maxGroupSize
    if (minGroup != null || maxGroup != null) {
        val text = when {
            minGroup != null && maxGroup != null -> stringResource(R.string.group_size_range_value).format(
                minGroup.toString(),
                maxGroup.toString()
            )

            minGroup != null -> stringResource(R.string.group_size_min_value).format(minGroup.toString())
            else -> stringResource(R.string.group_size_max_value).format(maxGroup.toString())
        }
        ActivityItem(Icons.Outlined.Group, text)
    }
    activity.pets?.let {
        ActivityItem(
            Icons.Outlined.Pets,
            if (it) stringResource(R.string.pets_allowed) else stringResource(R.string.no_pets)
        )
    }
    activity.languages?.takeIf { it.isNotEmpty() }?.let {
        ActivityItem(Icons.Outlined.Language, stringResource(R.string.languages_value).format(it.joinToString(", ")))
    }
    activity.outdoors?.let {
        ActivityItem(
            Icons.Outlined.NaturePeople,
            if (it) stringResource(R.string.activity_outdoors) else stringResource(R.string.activity_indoors)
        )
    }
}

@Composable
fun ActivityItem(icon: ImageVector, text: String, color: Color? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.5f.pad),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color ?: MaterialTheme.colorScheme.onSurface
        )
        Text(text, color = color ?: MaterialTheme.colorScheme.onSurface)
    }
}

fun isAvailableToday(activity: Activity): Boolean {
    val schedule = activity.schedule ?: return true

    val now = ZonedDateTime.now()
    val activityOffset = activity.utcOffset ?: 0.0
    val activityZone = ZoneOffset.ofTotalSeconds((activityOffset * 3600).toInt())
    val activityLocalTime = now.withZoneSameInstant(activityZone)

    val dayOfWeek = activityLocalTime.dayOfWeek.value // 1=Mon ... 7=Sun  (note: JS was 0=Sun)
    // Adjust to match JS: 1=Sun ...7=Sat ? Wait, JS getDay: 0=Sun,1=Mon...6=Sat so +1 ->1=Sun..7=Sat
    val jsDayOfWeek = if (dayOfWeek == 7) 1 else dayOfWeek + 1
    val dayOfMonth = activityLocalTime.dayOfMonth
    val month = activityLocalTime.monthValue
    val year = activityLocalTime.year
    val daysInMonth = activityLocalTime.month.length(activityLocalTime.toLocalDate().isLeapYear)
    val week = ((dayOfMonth - 1) / 7) + 1

    val daysMatch = schedule.days.isNullOrEmpty() && schedule.weekdays.isNullOrEmpty() ||
            (schedule.days?.contains(dayOfMonth) == true ||
                    (dayOfMonth == daysInMonth && schedule.days?.contains(-1) == true) ||
                    schedule.weekdays?.contains(jsDayOfWeek) == true)

    val weeksMatch = schedule.weeks.isNullOrEmpty() || schedule.weeks?.contains(week) == true
    val monthsMatch = schedule.months.isNullOrEmpty() || schedule.months?.contains(month) == true
    val yearsMatch = schedule.years.isNullOrEmpty() || schedule.years?.contains(year) == true

    return daysMatch && weeksMatch && monthsMatch && yearsMatch
}

fun formatSchedule(activity: Activity): String {
    val hours = activity.schedule?.hours
    if (hours.isNullOrEmpty()) return "Available today"

    val activityOffset = activity.utcOffset ?: 0.0
    val browserOffsetHours = ZoneOffset.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds / 3600.0
    val offsetDiff = browserOffsetHours - activityOffset

    val durationMs = activity.duration

    val formattedTimes = hours.map { startHour ->
        val localStartHour = startHour + offsetDiff
        val normalizedStartHour = ((localStartHour % 24) + 24) % 24

        val startHourInt = normalizedStartHour.toInt()
        val startMinutes = ((normalizedStartHour - startHourInt) * 60).toInt()

        val displayStartHour = if (startHourInt == 0) 12 else if (startHourInt > 12) startHourInt - 12 else startHourInt
        val startAmPm = if (startHourInt < 12) "a.m." else "p.m."
        val startTime = "$displayStartHour:${startMinutes.toString().padStart(2, '0')} $startAmPm"

        if (durationMs == null) {
            startTime
        } else {
            val endLocalHour = localStartHour + (durationMs / 1000.0 / 60.0 / 60.0)
            val normalizedEndHour = ((endLocalHour % 24) + 24) % 24

            val endHourInt = normalizedEndHour.toInt()
            val endMinutes = ((normalizedEndHour - endHourInt) * 60).toInt()

            val displayEndHour = if (endHourInt == 0) 12 else if (endHourInt > 12) endHourInt - 12 else endHourInt
            val endAmPm = if (endHourInt < 12) "a.m." else "p.m."

            val endTime = "$displayEndHour:${endMinutes.toString().padStart(2, '0')} $endAmPm"
            "$startTime to $endTime"
        }
    }

    return formattedTimes.joinToString(", ")
}
