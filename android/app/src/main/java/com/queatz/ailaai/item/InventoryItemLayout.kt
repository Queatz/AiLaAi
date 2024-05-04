package com.queatz.ailaai.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.formatDistance
import com.queatz.ailaai.extensions.isExpired
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.InventoryItemExtended
import kotlinx.datetime.Clock

@Composable
fun InventoryItemLayout(inventoryItem: InventoryItemExtended, quantity: Double? = null, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = Modifier
            .aspectRatio(.75f)
            .alpha(if (inventoryItem.inventoryItem!!.isExpired) 0.5f else 1.0f)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable {
                onClick()
            }
            .padding(1.pad)
    ) {
        AsyncImage(
            model = inventoryItem.item?.photo?.let { api.url(it) },
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .requiredSize(64.dp)
                .clip(MaterialTheme.shapes.large)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                listOfNotNull(
                    inventoryItem.item?.name ?: "",
                    ((quantity ?: inventoryItem.inventoryItem!!.quantity)?.format() ?: "0").let { "x$it" },
                    inventoryItem.inventoryItem?.expiresAt?.let {
                        val now = Clock.System.now()

                        if (it < now) {
                            stringResource(R.string.expired)
                        } else {
                            stringResource(R.string.x_remaining, it.formatDistance())
                        }.let {
                            "• $it"
                        }
                    }
                ).joinToString(" "),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
