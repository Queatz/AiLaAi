package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.pad

@Composable
fun PeriodEmpty() {
    Row {
        Text(
            "",
            modifier = Modifier.width(60.dp)
                .padding(
                    start = 1.pad,
                    top = 1.pad,
                    bottom = 1.pad
                ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            stringResource(R.string.nothing_scheduled),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = .5f),
            modifier = Modifier
                .padding(1.pad)
                .weight(1f)
        )
    }
}
