package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import app.ailaai.api.createInvite
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Invite
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInviteDialog(
    onDismissRequest: () -> Unit,
    groupId: String,
    onInviteCreated: (Invite) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var hasTotal by rememberStateOf(false)
    var total by rememberStateOf(10)
    var expires by rememberStateOf(false)

    // Get tomorrow's date
    val tomorrow = Clock.System.now().plus(kotlin.time.Duration.parse("24h"))
    val tomorrowLocal = tomorrow.toLocalDateTime(TimeZone.currentSystemDefault())

    var expiresDate by rememberStateOf(tomorrowLocal.date.toString())
    var expiresTime: String by rememberStateOf("${tomorrowLocal.hour.toString().padStart(2, '0')}:${tomorrowLocal.minute.toString().padStart(2, '0')}")

    var about by rememberStateOf("")
    var isCreating by rememberStateOf(false)
    var showCopyLinkDialog by rememberStateOf(false)
    var createdInviteCode by rememberStateOf("")

    // Date and time pickers
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = tomorrow.toEpochMilliseconds()
    )

    val timeState = rememberTimePickerState(
        initialHour = tomorrowLocal.hour,
        initialMinute = tomorrowLocal.minute
    )

    // Show date picker dialog
    var showDatePicker by rememberStateOf(false)
    var showTimePicker by rememberStateOf(false)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                        expiresDate = date.date.toString()
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                expiresTime = "${timeState.hour.toString().padStart(2, '0')}:${timeState.minute.toString().padStart(2, '0')}"
                showTimePicker = false
            }
        ) {
            TimePicker(state = timeState)
        }
    }

    if (showCopyLinkDialog) {
        CopyLinkDialog(
            onDismissRequest = {
                showCopyLinkDialog = false
                onDismissRequest()
            },
            code = createdInviteCode
        )
    }

    DialogBase(onDismissRequest) {
        DialogLayout(
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1.pad)
                ) {
                    OutlinedTextField(
                        value = about,
                        onValueChange = { about = it },
                        label = { Text(stringResource(R.string.description_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        shape = MaterialTheme.shapes.large
                    )

                    Spacer(modifier = Modifier.height(1.pad))

                    Check(
                        checked = hasTotal,
                        onCheckChange = { hasTotal = it },
                        label = { Text(stringResource(R.string.multiple_uses)) }
                    )

                    if (hasTotal) {
                        Spacer(modifier = Modifier.height(1.pad))
                        OutlinedTextField(
                            value = total.toString(),
                            onValueChange = { 
                                total = it.toIntOrNull() ?: 0
                            },
                            label = { Text(stringResource(R.string.number_of_uses)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.large
                        )
                    }

                    Spacer(modifier = Modifier.height(1.pad))

                    Check(
                        checked = expires,
                        onCheckChange = { expires = it },
                        label = { Text(stringResource(R.string.expires)) }
                    )

                    if (expires) {
                        Spacer(modifier = Modifier.height(1.pad))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.pad),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(expiresDate)
                            }

                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(expiresTime)
                            }
                        }
                    }
                }
            },
            actions = {
                TextButton(
                    onClick = { onDismissRequest() }
                ) {
                    Text(stringResource(R.string.cancel))
                }

                TextButton(
                    onClick = {
                        isCreating = true
                        coroutineScope.launch {
                            // Parse date and time
                            val expiryInstant = if (expires) {
                                val dateTimeParts = expiresDate.split("-")
                                val timeParts = expiresTime.split(":")

                                val year = dateTimeParts[0].toInt()
                                val month = dateTimeParts[1].toInt()
                                val day = dateTimeParts[2].toInt()
                                val hour = timeParts[0].toInt()
                                val minute = timeParts[1].toInt()

                                val calendar = Calendar.getInstance()
                                calendar.set(year, month - 1, day, hour, minute, 0)
                                calendar.set(Calendar.MILLISECOND, 0)

                                Instant.fromEpochMilliseconds(calendar.timeInMillis)
                            } else {
                                null
                            }

                            api.createInvite(
                                invite = Invite(
                                    group = groupId,
                                    about = about.notBlank,
                                    expiry = expiryInstant,
                                    total = if (hasTotal) total.takeIf { it > 0 } else null,
                                ),
                                onError = {
                                    isCreating = false
                                    context.showDidntWork()
                                }
                            ) { invite ->
                                isCreating = false
                                onInviteCreated(invite)
                                invite.code?.let { code ->
                                    createdInviteCode = code
                                    showCopyLinkDialog = true
                                } ?: onDismissRequest()
                            }
                        }
                    },
                    enabled = !isCreating
                ) {
                    Text(stringResource(R.string.create))
                }
            }
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    )
}
