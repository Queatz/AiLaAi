package com.queatz.ailaai.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.TradeExtended

@Composable
fun TradesDialog(
    onDismissRequest: () -> Unit,
    trades: List<TradeExtended>,
    onTrade: (TradeExtended) -> Unit
) {
    DialogBase(
        {
            onDismissRequest()
        }
    ) {
        DialogLayout(
            scrollable = false,
            content = {
                Text(
                    pluralStringResource(R.plurals.x_active_trades, trades.size, trades.size),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(bottom = 1.pad)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(1.pad),
                    modifier = Modifier
                        .weight(1f, fill = false)
                ) {
                    items(trades, key = { it.trade!!.id!! }) {
                        ActiveTradeItem(it) {
                            onTrade(it)
                        }
                    }
                }
            },
            actions = {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
