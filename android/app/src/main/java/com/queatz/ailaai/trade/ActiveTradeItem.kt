package com.queatz.ailaai.trade

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.TradeExtended

@Composable
fun ActiveTradeItem(trade: TradeExtended, onClick: () -> Unit) {
    val me = me

     Card(
         onClick = onClick,
         shape = MaterialTheme.shapes.large,
         modifier = Modifier
             .fillMaxWidth()
     ) {
         Column(
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(1.pad)
         ) {
             Text(
                 trade.people!!.filter { it.id != me?.id }.map { it.name ?: stringResource(R.string.someone) }
                     .joinToString(),
                 style = MaterialTheme.typography.titleLarge
             )
             Text(
                 listOf(
                     trade.trade!!.createdAt!!.timeAgo(),
                     pluralStringResource(R.plurals.x_items, trade.inventoryItems!!.size, trade.inventoryItems!!.size)
                 ).joinToString(" • ")
             )
         }
     }
}
