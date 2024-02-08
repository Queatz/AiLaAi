package com.queatz.ailaai.trade

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.rememberStateOf
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
    val me = me ?: return

    val confirmedByMe = trade?.trade?.members?.any { it.person == me.id!! && it.confirmed == true } == true
    val enableConfirm = confirmedByMe || trade?.trade?.members?.any { it.items!!.isNotEmpty() } == true

    fun reload() {
        scope.launch {
            api.trade(tradeId) {
                trade = it
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
}
