package com.queatz.ailaai.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.formatItemQuantity
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.ItemExtended

@Composable
fun ItemLayout(item: ItemExtended, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable {
                onClick()
            }
            .padding(1.pad)
    ) {
        AsyncImage(
            model = item.item?.photo?.let { api.url(it) },
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .requiredSize(64.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )
        Column {
            Text(item.item?.name ?: "")
            Text(
                // todo translate
                "Inventory: ${item.inventory?.formatItemQuantity()}   Circulating: ${item.circulating?.formatItemQuantity()}   Expired: ${item.expired?.formatItemQuantity()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
