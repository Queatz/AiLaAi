package components

import Styles
import androidx.compose.runtime.Composable
import appString
import com.queatz.db.Activity
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

@Composable
fun CardActivity(activity: Activity) {
    Div({
        classes(Styles.cardActivity)
    }) {
        val schedule = activity.schedule
        if (schedule != null) {
            val isAvailable = isAvailableToday(activity)
            if (isAvailable) {
                ActivityItem("calendar_today", formatSchedule(activity) + " " + appString { today })
            } else {
                ActivityItem("calendar_today", appString { notAvailableToday }, Styles.colors.gray)
            }
        }
        activity.duration?.let {
            ActivityItem("schedule", "Duration: ${it / 1000 / 60} minutes")
        }
        val minAge = activity.minAge
        val maxAge = activity.maxAge
        if (minAge != null || maxAge != null) {
            val text = when {
                minAge != null && maxAge != null -> "Age: $minAge - $maxAge"
                minAge != null -> "Age: $minAge+"
                else -> "Age: up to $maxAge"
            }
            ActivityItem("face", text)
        }
        val minGroup = activity.minGroupSize
        val maxGroup = activity.maxGroupSize
        if (minGroup != null || maxGroup != null) {
            val text = when {
                minGroup != null && maxGroup != null -> "Group size: $minGroup - $maxGroup"
                minGroup != null -> "Group size: $minGroup+"
                else -> "Group size: up to $maxGroup"
            }
            ActivityItem("group", text)
        }
        activity.pets?.let { ActivityItem("pets", if (it) "Pets allowed" else "No pets") }
        activity.languages?.takeIf { it.isNotEmpty() }?.let {
            ActivityItem("language", "Languages: ${it.joinToString(", ")}")
        }
        activity.outdoors?.let { ActivityItem("nature", if (it) "Outdoors" else "Indoors") }
    }
}

@Composable
fun ActivityItem(iconName: String, text: String, color: CSSColorValue? = null) {
    Div({
        style {
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            gap(.5.r)
            color?.let { color(it) }
        }
    }) {
        Icon(iconName)
        Text(text)
    }
}

fun isAvailableToday(activity: Activity): Boolean {
    val schedule = activity.schedule ?: return true
    
    val now = Date()
    val utcTime = now.getTime() + (now.getTimezoneOffset() * 60 * 1000)
    val activityOffsetMs = (activity.utcOffset ?: 0.0) * 60 * 60 * 1000
    val activityLocalTime = Date(utcTime + activityOffsetMs)
    
    val dayOfWeek = activityLocalTime.getDay() + 1 // 1=Sun...7=Sat
    val dayOfMonth = activityLocalTime.getDate() // 1..31
    val month = activityLocalTime.getMonth() + 1 // 1..12
    val year = activityLocalTime.getFullYear() // 2026
    val daysInMonth = Date(year, month, 0).getDate()
    val week = ((dayOfMonth - 1) / 7) + 1

    val daysMatch = schedule.days.isNullOrEmpty() && schedule.weekdays.isNullOrEmpty() ||
            (schedule.days?.contains(dayOfMonth) == true ||
            (dayOfMonth == daysInMonth && schedule.days?.contains(-1) == true) ||
            schedule.weekdays?.contains(dayOfWeek) == true)
            
    val weeksMatch = schedule.weeks.isNullOrEmpty() || schedule.weeks?.contains(week) == true
    val monthsMatch = schedule.months.isNullOrEmpty() || schedule.months?.contains(month) == true
    val yearsMatch = schedule.years.isNullOrEmpty() || schedule.years?.contains(year) == true

    return daysMatch && weeksMatch && monthsMatch && yearsMatch
}

fun formatSchedule(activity: Activity): String {
    val hours = activity.schedule?.hours
    if (hours.isNullOrEmpty()) return "Available today"

    val activityOffset = activity.utcOffset ?: 0.0
    val browserOffsetMinutes = kotlin.js.Date().getTimezoneOffset()
    val browserOffsetHours = -browserOffsetMinutes / 60.0
    val offsetDiff = browserOffsetHours - activityOffset

    val startHour = hours.first()
    val localStartHour = startHour + offsetDiff
    val normalizedStartHour = ((localStartHour % 24) + 24) % 24

    val startHourInt = normalizedStartHour.toInt()
    val startMinutes = ((normalizedStartHour - startHourInt) * 60).toInt()

    val displayStartHour = if (startHourInt == 0) 12 else if (startHourInt > 12) startHourInt - 12 else startHourInt
    val startAmPm = if (startHourInt < 12) "a.m." else "p.m."
    val startTime = "$displayStartHour:${startMinutes.toString().padStart(2, '0')} $startAmPm"

    val durationMs = activity.duration
    if (durationMs == null) {
        return startTime
    } else {
        val endLocalHour = localStartHour + (durationMs / 1000.0 / 60.0 / 60.0)
        val normalizedEndHour = ((endLocalHour % 24) + 24) % 24

        val endHourInt = normalizedEndHour.toInt()
        val endMinutes = ((normalizedEndHour - endHourInt) * 60).toInt()

        val displayEndHour = if (endHourInt == 0) 12 else if (endHourInt > 12) endHourInt - 12 else endHourInt
        val endAmPm = if (endHourInt < 12) "a.m." else "p.m."

        val endTime = "$displayEndHour:${endMinutes.toString().padStart(2, '0')} $endAmPm"
        return "$startTime to $endTime"
    }
}
