package com.queatz.ailaai.group

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.db.Message
import com.queatz.db.TradeAttachment
import kotlinx.serialization.encodeToString

@Composable
fun SendTradeDialog(
    onDismissRequest: () -> Unit,
    tradeId: String
) {
    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)
    val me = me

    SendMessageDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.send_trade),
        confirmFormatter = defaultConfirmFormatter(
            R.string.send_trade,
            R.string.send_trade_to_group,
            R.string.send_trade_to_groups,
            R.string.send_trade_to_x_groups
        ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) },
        message = Message(attachment = json.encodeToString(TradeAttachment(tradeId)))
    )
}
