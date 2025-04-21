package com.queatz.ailaai.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.db.ReminderStickiness

@Composable
fun ReminderStickiness.label(): String {
    return when (this) {
        ReminderStickiness.None -> stringResource(R.string.none)
        ReminderStickiness.Hourly -> stringResource(R.string.hourly)
        ReminderStickiness.Daily -> stringResource(R.string.daily)
        ReminderStickiness.Weekly -> stringResource(R.string.weekly)
        ReminderStickiness.Monthly -> stringResource(R.string.monthly)
        ReminderStickiness.Yearly -> stringResource(R.string.yearly)
    }
}
