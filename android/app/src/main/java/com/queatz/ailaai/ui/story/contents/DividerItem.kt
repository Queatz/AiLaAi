package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import com.queatz.ailaai.ui.theme.pad

fun LazyGridScope.dividerItem() {
    item(span = { GridItemSpan(maxLineSpan) }) {
        DisableSelection {
            Icon(
                Icons.Outlined.Flare,
                null,
                modifier = Modifier.padding(2.pad)
            )
        }
    }
}
