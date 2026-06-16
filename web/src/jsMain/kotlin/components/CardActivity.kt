package components

import Styles
import androidx.compose.runtime.Composable
import appString
import appStringShort
import application
import com.queatz.db.Activity
import com.queatz.db.Card
import com.queatz.db.Parking
import com.queatz.db.formatPrice
import format
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

@Composable
fun CardActivity(activity: Activity, card: Card? = null) {
    Div({
        classes(Styles.cardActivity)
    }) {
        val schedule = activity.schedule
        if (schedule != null) {
            val isAvailable = isAvailableToday(activity)
            if (isAvailable) {
                val timesStr = formatSchedule(activity)
                ActivityItem(
                    "calendar_today",
                    if (timesStr.isNotBlank()) "$timesStr " + appString { todayInline } else appString { availableTodayFallback })
            } else {
                val nextDate = nextAvailableDate(activity)
                ActivityItem(
                    iconName = "calendar_today",
                    text = if (nextDate != null) {
                        appString { nextAvailableDate }.format(nextDate)
                    } else {
                        appString { notAvailableToday }
                    },
                    color = Styles.colors.gray
                )
            }
        }
        activity.duration?.let {
            val minutes = (it / 1000 / 60).toInt()
            val text = when {
                minutes > 90 -> {
                    val h = minutes / 60
                    val m = minutes % 60
                    if (m > 0) {
                        appString { durationHoursMinutes }.format(h.toString(), m.toString())
                    } else {
                        appString { durationHours }.format(h.toString())
                    }
                }

                else -> appString { durationMinutes }.format(minutes.toString())
            }
            ActivityItem("schedule", text)
        }
        card?.let {
            val price = it.formatPrice { appStringShort } ?: appString { free }
            ActivityItem("payments", price)
        }
        val minAge = activity.minAge
        val maxAge = activity.maxAge
        if (minAge != null || maxAge != null) {
            val text = when {
                minAge != null && maxAge != null -> appString { ageRangeValue }.format(
                    minAge.toString(),
                    maxAge.toString()
                )

                minAge != null -> appString { ageMinValue }.format(minAge.toString())
                else -> appString { ageMaxValue }.format(maxAge.toString())
            }
            ActivityItem("face", text)
        }
        val minGroup = activity.minGroupSize
        val maxGroup = activity.maxGroupSize
        if (minGroup != null || maxGroup != null) {
            val text = when {
                minGroup != null && maxGroup != null -> appString { groupSizeRangeValue }.format(
                    minGroup.toString(),
                    maxGroup.toString()
                )

                minGroup != null -> appString { groupSizeMinValue }.format(minGroup.toString())
                else -> appString { groupSizeMaxValue }.format(maxGroup.toString())
            }
            ActivityItem("group", text)
        }
        activity.pets?.let { ActivityItem("pets", if (it) appString { petsAllowed } else appString { noPets }) }
        activity.languages?.takeIf { it.isNotEmpty() }?.let {
            ActivityItem("language", appString { languagesValue }.format(it.joinToString(", ")))
        }
        activity.outdoors?.let {
            ActivityItem(
                "nature",
                if (it) appString { activityOutdoors } else appString { activityIndoors })
        }
        activity.parking?.let {
            val text = when (it) {
                Parking.None -> appString { parkingAreaNone }
                Parking.Bike -> appString { parkingAreaBike }
                Parking.Motorbike -> appString { parkingAreaMotorbike }
                Parking.Car -> appString { parkingAreaCar }
            }
            val icon = when (it) {
                Parking.None -> "local_parking"
                Parking.Bike -> "pedal_bike"
                Parking.Motorbike -> "moped"
                Parking.Car -> "directions_car"
            }
            ActivityItem(icon, text)
        }
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

fun nextAvailableDate(activity: Activity): String? {
    val activityOffset = activity.utcOffset ?: 0.0
    val activityOffsetMs = activityOffset * 60 * 60 * 1000
    val now = Date()
    val utcTime = now.getTime() + (now.getTimezoneOffset() * 60 * 1000)
    var candidateTime = utcTime + activityOffsetMs + (24 * 60 * 60 * 1000)
    repeat(365) {
        val candidate = Date(candidateTime)
        val dayOfWeek = candidate.getDay() + 1
        val dayOfMonth = candidate.getDate()
        val month = candidate.getMonth() + 1
        val year = candidate.getFullYear()
        val daysInMonth = Date(year, month, 0).getDate()
        val week = ((dayOfMonth - 1) / 7) + 1
        val schedule = activity.schedule ?: return null
        val daysMatch = schedule.days.isNullOrEmpty() && schedule.weekdays.isNullOrEmpty() ||
                (schedule.days?.contains(dayOfMonth) == true ||
                        (dayOfMonth == daysInMonth && schedule.days?.contains(-1) == true) ||
                        schedule.weekdays?.contains(dayOfWeek) == true)
        val weeksMatch = schedule.weeks.isNullOrEmpty() || schedule.weeks?.contains(week) == true
        val monthsMatch = schedule.months.isNullOrEmpty() || schedule.months?.contains(month) == true
        val yearsMatch = schedule.years.isNullOrEmpty() || schedule.years?.contains(year) == true
        if (daysMatch && weeksMatch && monthsMatch && yearsMatch) {
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val dateStr = "${monthNames[candidate.getMonth()]} ${candidate.getDate()}"
            val timesStr = formatSchedule(activity)
            return if (timesStr.isNotBlank()) "$dateStr, $timesStr" else dateStr
        }
        candidateTime += 24 * 60 * 60 * 1000
    }
    return null
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
    if (hours.isNullOrEmpty()) return ""

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

fun activityTime(activity: Activity): String {
    return if (isAvailableToday(activity)) {
        val timesStr = formatSchedule(activity)
        if (timesStr.isNotBlank()) "$timesStr " + application.appString { todayInline } else application.appString { availableTodayFallback }
    } else {
        val nextDate = nextAvailableDate(activity)
        if (nextDate != null) {
            application.appString { nextAvailableDate }.format(nextDate)
        } else {
            application.appString { notAvailableToday }
        }

    }
}

fun Card.activityDescription(
    includeTime: Boolean = true,
    full: Boolean = true,
): String {
    val activityDetails = activity?.let { activity ->
        val details = mutableListOf<String>()
        val schedule = activity.schedule
        if (schedule != null && includeTime) {
            details.add(activityTime(activity))
        }
        activity.duration?.let {
            val minutes = (it / 1000 / 60).toInt()
            val text = when {
                minutes > 90 -> {
                    val h = minutes / 60
                    val m = minutes % 60
                    if (m > 0) {
                        application.appString { durationHoursMinutes }.format(h.toString(), m.toString())
                    } else {
                        application.appString { durationHours }.format(h.toString())
                    }
                }

                else -> application.appString { durationMinutes }.format(minutes.toString())
            }
            details.add(text)
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
        activity.parking?.let {
            details.add(
                when (it) {
                    Parking.None -> application.appString { parkingAreaNone }
                    Parking.Bike -> application.appString { parkingAreaBike }
                    Parking.Motorbike -> application.appString { parkingAreaMotorbike }
                    Parking.Car -> application.appString { parkingAreaCar }
                }
            )
        }
        details.joinToString(", ")
    }

    val price = formatPrice { appStringShort }
    val description = if (full) getConversation().message.takeIf { it.isNotBlank() } else null

    return listOfNotNull(
        activityDetails?.takeIf { it.isNotBlank() },
        price?.takeIf { it.isNotBlank() },
        categories?.firstOrNull()?.takeIf { it.isNotBlank() },
        description?.takeIf { it.isNotBlank() }
    ).joinToString(", ")
}
