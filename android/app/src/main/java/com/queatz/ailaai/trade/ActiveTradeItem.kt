package com.queatz.ailaai.trade

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.TradeExtended

@Composable
fun ActiveTradeItem(trade: TradeExtended, onClick: () -> Unit) {
    val me = me

     OutlinedCard(
         onClick = onClick,
         shape = MaterialTheme.shapes.large,
         modifier = Modifier
             .fillMaxWidth()
     ) {
         Row(
             horizontalArrangement = Arrangement.spacedBy(1.pad),
             verticalAlignment = Alignment.CenterVertically,
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(1.5f.pad)
         ) {
             GroupPhoto(
                 photos = trade.people!!.filter { it.id != me?.id }.map { it.contactPhoto() },
                 padding = 0.pad
             )
             Column(
                 modifier = Modifier
                     .weight(1f)
             ) {
                 Text(
                     trade.people!!.filter { it.id != me?.id }.map { it.name ?: stringResource(R.string.someone) }
                         .joinToString(),
                     style = MaterialTheme.typography.titleLarge
                 )
                 Text(
                     bulletedString(
                         trade.trade!!.createdAt!!.timeAgo(),
                         pluralStringResource(R.plurals.x_items, trade.inventoryItems!!.size, trade.inventoryItems!!.size)
                     )
                 )
             }
             if (trade.trade?.members?.any { it.person == me?.id && it.confirmed == true } == true) {
                 Icon(
                     Icons.Outlined.Check,
                     null,
                     tint = MaterialTheme.colorScheme.primary,
                     modifier = Modifier
                         .padding(horizontal = 1.pad)
                 )
             }
         }
     }
}
