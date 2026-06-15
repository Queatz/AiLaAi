package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.startOfDay
import com.queatz.ailaai.schedule.DurationDialog
import com.queatz.ailaai.schedule.ScheduleReminderDialog
import com.queatz.ailaai.schedule.scheduleText
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Activity
import com.queatz.db.Parking
import com.queatz.db.Reminder
import kotlinx.coroutines.launch
import java.util.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ActivityDialog(
    onDismissRequest: () -> Unit,
    activity: Activity?,
    onSave: suspend (Activity?) -> Unit
) {
    val scope = rememberCoroutineScope()
    var active by rememberStateOf(activity?.active ?: true)
    var minAge by rememberStateOf(activity?.minAge?.toString() ?: "")
    var maxAge by rememberStateOf(activity?.maxAge?.toString() ?: "")
    var minGroupSize by rememberStateOf(activity?.minGroupSize?.toString() ?: "")
    var maxGroupSize by rememberStateOf(activity?.maxGroupSize?.toString() ?: "")
    var pets by rememberStateOf(activity?.pets ?: false)
    var outdoors by rememberStateOf(activity?.outdoors ?: false)
    var parking by rememberStateOf(activity?.parking)
    var languages by rememberStateOf(activity?.languages?.joinToString(", ") ?: "")
    var duration by rememberStateOf(activity?.duration ?: 0L)
    var schedule by rememberStateOf(activity?.schedule)
    var timezone by rememberStateOf(activity?.timezone ?: TimeZone.getDefault().id)
    var utcOffset by rememberStateOf(activity?.utcOffset ?: (TimeZone.getDefault().rawOffset.toDouble() / 3600000.0))
    var showScheduleDialog by rememberStateOf(false)
    var showDurationDialog by rememberStateOf(false)
    var showTimezoneDialog by rememberStateOf(false)
    var showRemoveConfirmation by rememberStateOf(false)

    val scrollState = rememberScrollState()

    if (showScheduleDialog) {
        ScheduleReminderDialog(
            onDismissRequest = { showScheduleDialog = false },
            initialReminder = Reminder(
                start = Clock.System.now().startOfDay(),
                schedule = schedule
            ),
            includeStickiness = false,
            onUpdate = {
                schedule = it.schedule
                showScheduleDialog = false
            }
        )
    }

    if (showDurationDialog) {
        DurationDialog(
            onDismissRequest = { showDurationDialog = false },
            initialDuration = duration,
            onDuration = {
                duration = it
                showDurationDialog = false
            }
        )
    }

    if (showTimezoneDialog) {
        ChooseTimezoneDialog(
            onDismissRequest = { showTimezoneDialog = false },
            preselect = timezone,
            onTimezone = {
                if (it != null) {
                    timezone = it
                    val tz = TimeZone.getTimeZone(it)
                    utcOffset = tz.rawOffset.toDouble() / 3600000.0
                }
                showTimezoneDialog = false
            }
        )
    }

    if (showRemoveConfirmation) {
        Alert(
            onDismissRequest = { showRemoveConfirmation = false },
            title = stringResource(R.string.remove_activity),
            text = stringResource(R.string.you_cannot_undo_this_reminder), // Or another suitable string
            confirmButton = stringResource(R.string.remove),
            dismissButton = stringResource(R.string.cancel),
            confirmColor = MaterialTheme.colorScheme.error
        ) {
            scope.launch {
                onSave(null)
                onDismissRequest()
            }
        }
    }

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
                .verticalScroll(scrollState)
        ) {
            Text(
                stringResource(R.string.activity),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 2.pad)
            )

            Column(verticalArrangement = Arrangement.spacedBy(1.pad)) {
                Check(active, { active = it }) {
                    Text(stringResource(R.string.active))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(1.pad)) {
                    OutlinedTextField(
                        minAge,
                        { if (it.all { it.isDigit() }) minAge = it },
                        label = { Text(stringResource(R.string.min_age)) },
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        maxAge,
                        { if (it.all { it.isDigit() }) maxAge = it },
                        label = { Text(stringResource(R.string.max_age)) },
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(1.pad)) {
                    OutlinedTextField(
                        minGroupSize,
                        { if (it.all { it.isDigit() }) minGroupSize = it },
                        label = { Text(stringResource(R.string.min_group_size)) },
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        maxGroupSize,
                        { if (it.all { it.isDigit() }) maxGroupSize = it },
                        label = { Text(stringResource(R.string.max_group_size)) },
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    languages,
                    { languages = it },
                    label = { Text(stringResource(R.string.languages)) },
                    placeholder = { Text("e.g. English, Vietnamese") },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )

                Check(pets, { pets = it }) {
                    Text(stringResource(R.string.pets))
                }

                Check(outdoors, { outdoors = it }) {
                    Text(stringResource(R.string.outdoors))
                }

                Text(
                    stringResource(R.string.parking),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.pad),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        Parking.None to stringResource(R.string.parking_none),
                        Parking.Bike to stringResource(R.string.parking_bike),
                        Parking.Motorbike to stringResource(R.string.parking_motorbike),
                        Parking.Car to stringResource(R.string.parking_car)
                    ).forEach { (option, label) ->
                        if (parking == option) {
                            OutlinedButton(
                                onClick = { parking = null },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(label)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { parking = option }
                            ) {
                                Text(label)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showDurationDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 1.pad)
                    )
                    Text(
                        if (duration > 0) duration.milliseconds.format() else stringResource(R.string.duration)
                    )
                }

                OutlinedButton(
                    onClick = { showTimezoneDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(timezone)
                }

                OutlinedButton(
                    onClick = { showScheduleDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (schedule == null) stringResource(R.string.schedule) else Reminder(
                            start = Clock.System.now().startOfDay(),
                            schedule = schedule
                        ).scheduleText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.pad),
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        showRemoveConfirmation = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.remove))
                }

                Spacer(Modifier.weight(1f))

                TextButton(onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = {
                    scope.launch {
                        onSave(Activity(
                            active = active,
                            minAge = minAge.toIntOrNull(),
                            maxAge = maxAge.toIntOrNull(),
                            minGroupSize = minGroupSize.toIntOrNull(),
                            maxGroupSize = maxGroupSize.toIntOrNull(),
                            languages = languages.split(",").map { it.trim() }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() },
                            duration = duration.takeIf { it > 0 },
                            pets = pets.takeIf { it },
                            outdoors = outdoors.takeIf { it },
                            parking = parking,
                            schedule = schedule,
                            timezone = timezone,
                            utcOffset = utcOffset
                        ))
                        onDismissRequest()
                    }
                }) {
                    Text(stringResource(R.string.update))
                }
            }
        }
    }
}
