package com.queatz.ailaai.item

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.format
import com.queatz.db.ItemExtended

@Composable
fun MyItemLayout(
    item: ItemExtended,
    onClick: () -> Unit,
) {
    ItemLayout(
        item = item.item!!,
        hint = bulletedString(
            stringResource(R.string.x_in_inventory, item.inventory!!.format()),
            stringResource(R.string.x_circulating, item.circulating!!.format()),
            if (item.item?.lifespan != null) stringResource(R.string.x_expired, item.expired!!.format()) else null,
        ),
        onClick = onClick
    )
}
