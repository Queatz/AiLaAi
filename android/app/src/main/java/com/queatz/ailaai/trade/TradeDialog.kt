package com.queatz.ailaai.trade

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cancelTrade
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.db.TradeExtended
import confirmTrade
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import trade
import unconfirmTrade

@Composable
fun TradeDialog(
    onDismissRequest: () -> Unit,
    tradeId: String
) {
    val scope = rememberCoroutineScope()
    var trade by rememberStateOf<TradeExtended?>(null)
    var showCancelDialog by rememberStateOf(false)
    val me = me ?: return
    val context = LocalContext.current

    val confirmedByMe = trade?.trade?.members?.any { it.person == me.id!! && it.confirmed == true } == true
    val enableConfirm = confirmedByMe || trade?.trade?.members?.any { it.items!!.isNotEmpty() } == true

    fun reload() {
        scope.launch {
            api.trade(tradeId) {
                trade = it
            }
        }
    }

    fun cancel() {
        scope.launch {
            api.cancelTrade(tradeId) {
                context.toast(R.string.trade_cancelled)
                onDismissRequest()
            }
        }
    }

    fun confirmUnconfirm() {
        if (confirmedByMe) {
            scope.launch {
                api.unconfirmTrade(tradeId) {
                    trade = it
                }
            }
        } else {
            scope.launch {
                api.confirmTrade(tradeId, trade!!.trade!!) {
                    trade = it
                }
            }
        }
    }

    LaunchedEffect(tradeId) {
        trade = null
        reload()
    }

    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            content = {
                Text(json.encodeToString(trade))
            },
            actions = {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
                TextButton(
                    {
                        showCancelDialog = true
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    {
                        confirmUnconfirm()
                    },
                    enabled = enableConfirm
                ) {
                    Text(
                        stringResource(
                            if (confirmedByMe) {
                                R.string.unconfirm
                            } else {
                                R.string.confirm
                            }
                        )
                    )
                }
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            {
                showCancelDialog = false
            },
            title = {
                Text(stringResource(R.string.cancel_this_trade))
            },
            confirmButton = {
                TextButton(
                    {
                        cancel()
                        showCancelDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel_trade))
                }
            },
            dismissButton = {
                TextButton(
                    {
                        showCancelDialog = false
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
