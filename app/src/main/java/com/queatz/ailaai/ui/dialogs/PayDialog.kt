package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.appString
import com.queatz.ailaai.extensions.appStringShort
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toggle
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.GroupMember
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Pay
import com.queatz.db.PayFrequency
import kotlinx.coroutines.launch

@Composable
fun PayDialog(
    onDismissRequest: () -> Unit,
    defaultPay: String?,
    defaultFrequency: PayFrequency?,
    onSave: suspend (Pay?) -> Unit
) {
    val scope = rememberCoroutineScope()
    var pay by rememberStateOf(defaultPay ?: "")
    var payFrequency by rememberStateOf(defaultFrequency)
    val scrollState = rememberScrollState()

    DialogBase(onDismissRequest, dismissable = false, modifier = Modifier.wrapContentHeight()) {
        Column(
            modifier = Modifier
                .padding(3.pad)
                .verticalScroll(scrollState)
        ) {
            Text(
                stringResource(R.string.pay),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 1.pad)
            )
            OutlinedTextField(
                pay,
                onValueChange = {
                    pay = it
                },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 1.pad)
            )
            Column {
                PayFrequency.entries.forEach {
                    val isSelected = payFrequency == it
                    GroupMember(
                        null,
                        it.appString,
                        null,
                        isSelected
                    ) {
                        payFrequency = if (payFrequency == it) null else it
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                var disableUpdateButton by rememberStateOf(false)

                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    {
                        scope.launch {
                            disableUpdateButton = true
                            onSave(
                                Pay(
                                    pay = pay,
                                    frequency = payFrequency
                                )
                            )
                            disableUpdateButton = false
                            onDismissRequest()
                        }
                    },
                    enabled = !disableUpdateButton
                ) {
                    Text(stringResource(R.string.update))
                }
            }
        }
    }
}
