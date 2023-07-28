package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.*
import com.queatz.ailaai.api.sendReport
import com.queatz.ailaai.data.Report
import com.queatz.ailaai.data.ReportType
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun ReportDialog(entity: String, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    var reportType by rememberStateOf<ReportType?>(null)
    var urgent by rememberStateOf(false)
    TextFieldDialog(
        onDismissRequest,
        title = stringResource(R.string.report),
        button = stringResource(R.string.send_report),
        showDismiss = true,
        placeholder = stringResource(R.string.add_details),
        requireModification = false,
        extraContent = {
            enumValues<ReportType>().forEach {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = reportType == it,
                        onClick = {
                            reportType = it
                        }
                    )
                    Column(
                        Modifier.clickable(
                            MutableInteractionSource(),
                            null
                        ) {
                            reportType = it
                        }
                    ) {
                        Text(
                            when (it) {
                                ReportType.Safety -> {
                                    stringResource(R.string.personal_safety)
                                }
                                ReportType.Content -> {
                                    stringResource(R.string.content_issue)
                                }
                                ReportType.Spam -> {
                                    stringResource(R.string.spam_or_fake)
                                }
                                ReportType.Other -> {
                                    stringResource(R.string.other)
                                }
                            }
                        )
                        Text(
                            when (it) {
                                ReportType.Safety -> {
                                    stringResource(R.string.personal_safety_description)
                                }
                                ReportType.Content -> {
                                    stringResource(R.string.content_issue_description)
                                }
                                ReportType.Spam -> {
                                    stringResource(R.string.spam_or_fake_description)
                                }
                                ReportType.Other -> {
                                    stringResource(R.string.report_type_other_description)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    urgent,
                    { urgent = it }
                )
                Column(
                    Modifier.clickable(
                        MutableInteractionSource(),
                        null
                    ) {
                        urgent = !urgent
                    }
                ) {
                    Text(stringResource(R.string.this_is_urgent))
                }
            }
            Box(modifier = Modifier.height(PaddingDefault * 2))
        }
    ) { value ->
        api.sendReport(
            Report(
            entity = entity,
            reporterMessage = value,
            urgent = urgent,
            type = reportType ?: ReportType.Other
        )
        ) {
            context.toast(context.getString(R.string.thank_you))
            onDismissRequest()
        }
    }
}
