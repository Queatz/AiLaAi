package app.reminder

import androidx.compose.runtime.Composable
import app.page.ScheduleView
import appString
import lib.format
import lib.isToday
import lib.isTomorrow
import lib.isYesterday
import kotlin.js.Date

@Composable
fun Date.formatSecondary(view: ScheduleView) = when (view) {
    ScheduleView.Daily -> {
        format(this, "h:mm a")
    }

    ScheduleView.Weekly -> {
        format(this, "MMMM do, EEEE, h:mm a")
    }

    ScheduleView.Monthly -> {
        format(this, "do, EEEE, h:mm a")
    }

    ScheduleView.Yearly -> {
        format(this, "MMMM do, EEEE, h:mm a")
    }
}

@Composable
fun Date.formatTitle(view: ScheduleView) = when (view) {
    ScheduleView.Daily -> {
        (if (isToday(this)) "${appString { today }}, " else if (isYesterday(this)) "${appString { yesterday }}, " else if (isTomorrow(
                this
            )
        ) "${appString { tomorrow }}, " else "") + format(
            this,
            "EEEE, MMMM do"
        )
    }

    ScheduleView.Weekly -> {
        format(this, "EEEE, MMMM do")
    }

    ScheduleView.Monthly -> {
        format(this, "MMMM, yyyy")
    }

    ScheduleView.Yearly -> {
        format(this, "yyyy G")
    }
}
