package com.queatz.ailaai.schedule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.Dropdown

@Composable
fun ScheduleMenu(show: Boolean, onDismissRequest: () -> Unit, view: ScheduleView, onView: (ScheduleView) -> Unit) {
    Dropdown(show, onDismissRequest) {
        DropdownMenuItem({
            Text(stringResource(R.string.daily))
        }, trailingIcon = {
            if (view == ScheduleView.Daily) {
                Icon(Icons.Outlined.Check, null)
            }
        }, onClick = {
            onDismissRequest()
            onView(ScheduleView.Daily)
        })
        DropdownMenuItem({
            Text(stringResource(R.string.weekly))
        }, trailingIcon = {
            if (view == ScheduleView.Weekly) {
                Icon(Icons.Outlined.Check, null)
            }
        }, onClick = {
            onDismissRequest()
            onView(ScheduleView.Weekly)
        })
        DropdownMenuItem({
            Text(stringResource(R.string.monthly))
        }, trailingIcon = {
            if (view == ScheduleView.Monthly) {
                Icon(Icons.Outlined.Check, null)
            }
        }, onClick = {
            onDismissRequest()
            onView(ScheduleView.Monthly)
        })
        DropdownMenuItem({
            Text(stringResource(R.string.yearly))
        }, trailingIcon = {
            if (view == ScheduleView.Yearly) {
                Icon(Icons.Outlined.Check, null)
            }
        }, onClick = {
            onDismissRequest()
            onView(ScheduleView.Yearly)
        })
    }
}
