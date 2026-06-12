package components

import Styles
import androidx.compose.runtime.Composable
import appString
import application
import com.queatz.db.Activity
import com.queatz.db.Card
import com.queatz.db.formatPay
import format
import appStringShort
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
                ActivityItem("calendar_today", formatSchedule(activity) + " " + appString { todayInline })
            } else {
                ActivityItem("calendar_today", appString { notAvailableToday }, Styles.colors.gray)
            }
        }
        activity.duration?.let {
            ActivityItem("schedule", appString { durationMinutes }.format((it / 1000 / 60).toString()))
        }
        val minAge = activity.minAge
        val maxAge = activity.maxAge
        if (minAge != null || maxAge != null) {
            val text = when {
                minAge != null && maxAge != null -> appString { ageRangeValue }.format(minAge.toString(), maxAge.toString())
                minAge != null -> appString { ageMinValue }.format(minAge.toString())
                else -> appString { ageMaxValue }.format(maxAge.toString())
            }
            ActivityItem("face", text)
        }
        val minGroup = activity.minGroupSize
        val maxGroup = activity.maxGroupSize
        if (minGroup != null || maxGroup != null) {
            val text = when {
                minGroup != null && maxGroup != null -> appString { groupSizeRangeValue }.format(minGroup.toString(), maxGroup.toString())
                minGroup != null -> appString { groupSizeMinValue }.format(minGroup.toString())
                else -> appString { groupSizeMaxValue }.format(maxGroup.toString())
            }
            ActivityItem("group", text)
        }
        activity.pets?.let { ActivityItem("pets", if (it) appString { petsAllowed } else appString { noPets }) }
        activity.languages?.takeIf { it.isNotEmpty() }?.let {
            ActivityItem("language", appString { languagesValue }.format(it.joinToString(", ")))
        }
        activity.outdoors?.let { ActivityItem("nature", if (it) appString { activityOutdoors } else appString { activityIndoors }) }
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
            fontWeight("bold")
        }
    }) {
        Icon(iconName) {
            opacity(.5f)
        }
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
    if (hours.isNullOrEmpty()) return application.appString { availableTodayFallback }

    val activityOffset = activity.utcOffset ?: 0.0
    val browserOffsetMinutes = kotlin.js.Date().getTimezoneOffset()
    val browserOffsetHours = -browserOffsetMinutes / 60.0
    val offsetDiff = browserOffsetHours - activityOffset

    val durationMs = activity.duration

    val formattedTimes = hours.map { startHour ->
        val localStartHour = startHour + offsetDiff
        val normalizedStartHour = ((localStartHour % 24) + 24) % 24

        val startHourInt = normalizedStartHour.toInt()
        val startMinutes = ((normalizedStartHour - startHourInt) * 60).toInt()

        val displayStartHour = if (startHourInt == 0) 12 else if (startHourInt > 12) startHourInt - 12 else startHourInt
        val startAmPm = if (startHourInt < 12) application.appString { amLabel } else application.appString { pmLabel }
        val startTime = "$displayStartHour:${startMinutes.toString().padStart(2, '0')} $startAmPm"

        if (durationMs == null) {
            startTime
        } else {
            val endLocalHour = localStartHour + (durationMs / 1000.0 / 60.0 / 60.0)
            val normalizedEndHour = ((endLocalHour % 24) + 24) % 24

            val endHourInt = normalizedEndHour.toInt()
            val endMinutes = ((normalizedEndHour - endHourInt) * 60).toInt()

            val displayEndHour = if (endHourInt == 0) 12 else if (endHourInt > 12) endHourInt - 12 else endHourInt
            val endAmPm = if (endHourInt < 12) application.appString { amLabel } else application.appString { pmLabel }

            val endTime = "$displayEndHour:${endMinutes.toString().padStart(2, '0')} $endAmPm"
            "$startTime${application.appString { timeToSeparator }}$endTime"
        }
    }

    return formattedTimes.joinToString(", ")
}

fun Card.ogDescription(): String {
    val activityDetails = activity?.let { activity ->
        val details = mutableListOf<String>()
        val schedule = activity.schedule
        if (schedule != null) {
            if (isAvailableToday(activity)) {
                details.add(formatSchedule(activity) + " " + application.appString { todayInline })
            } else {
                details.add(application.appString { notAvailableToday })
            }
        }
        activity.duration?.let {
            details.add(application.appString { durationMinutes }.format((it / 1000 / 60).toString()))
        }
        val minAge = activity.minAge
        val maxAge = activity.maxAge
        if (minAge != null || maxAge != null) {
            details.add(
                when {
                    minAge != null && maxAge != null -> application.appString { ageRangeValue }.format(
                        minAge.toString(),
                        maxAge.toString()
                    )

                    minAge != null -> application.appString { ageMinValue }.format(minAge.toString())
                    else -> application.appString { ageMaxValue }.format(maxAge.toString())
                }
            )
        }
        val minGroup = activity.minGroupSize
        val maxGroup = activity.maxGroupSize
        if (minGroup != null || maxGroup != null) {
            details.add(
                when {
                    minGroup != null && maxGroup != null -> application.appString { groupSizeRangeValue }.format(
                        minGroup.toString(),
                        maxGroup.toString()
                    )

                    minGroup != null -> application.appString { groupSizeMinValue }.format(minGroup.toString())
                    else -> application.appString { groupSizeMaxValue }.format(maxGroup.toString())
                }
            )
        }
        activity.pets?.let {
            details.add(if (it) application.appString { petsAllowed } else application.appString { noPets })
        }
        activity.languages?.takeIf { it.isNotEmpty() }?.let {
            details.add(application.appString { languagesValue }.format(it.joinToString(", ")))
        }
        activity.outdoors?.let {
            details.add(if (it) application.appString { activityOutdoors } else application.appString { activityIndoors })
        }
        details.joinToString(", ")
    }

    val price = formatPay { appStringShort }
    val description = content

    return listOfNotNull(
        activityDetails?.takeIf { it.isNotBlank() },
        price?.takeIf { it.isNotBlank() },
        description?.takeIf { it.isNotBlank() }
    ).joinToString(", ")
}
