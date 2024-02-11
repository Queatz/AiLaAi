package com.queatz.ailaai.push

import android.content.Intent
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.schedule.asNaturalList
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.TradeEvent
import com.queatz.push.TradePushData

fun Push.receive(data: TradePushData) {
    if (
        latestEvent == Lifecycle.Event.ON_RESUME &&
        navController?.currentBackStackEntry?.destination?.route == "inventory"
    ) {
        return
    }


    // Don't notify notifications from myself, but do update latestMessage flow
    if (data.person.id == meId) {
        return
    }

    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        // todo go to specific trade
        "$appDomain/inventory".toUri(),
        context,
        MainActivity::class.java
    )

    // Todo needs to check if trade dialog is active
    send(
        deeplinkIntent,
        Notifications.Trade,
        groupKey = "trade/${data.trade.id}",
        title = when (data.event) {
            TradeEvent.Started -> context.getString(R.string.trade)
            TradeEvent.Updated -> context.getString(R.string.trade_updated)
            TradeEvent.Completed -> context.getString(R.string.trade_completed)
            TradeEvent.Cancelled -> context.getString(R.string.trade_cancelled)
        },
        text = data.people?.joinToString { it.name ?: context.getString(R.string.someone) } ?: ""
    )
}
