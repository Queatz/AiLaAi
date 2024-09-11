package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.ui.theme.pad
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

data class DateTimeSuggestion(
    val title: String,
    val date: Instant
)

@Composable
fun DateTimeSuggestions(modifier: Modifier = Modifier, onSelect: (Instant) -> Unit) {
    val state = rememberLazyListState()
    val now = Clock.System.now()

    val suggestions =
        listOf(
            DateTimeSuggestion(stringResource(R.string.today), now),
            DateTimeSuggestion(stringResource(R.string.tomorrow), now + 1.days),
            DateTimeSuggestion(stringResource(R.string.in_a_week), now + 7.days),
            DateTimeSuggestion(stringResource(R.string.in_a_month), now + 30.days),
            DateTimeSuggestion(stringResource(R.string.in_a_year), now + 365.days)
        )

    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier
            .fillMaxWidth()
            .horizontalFadingEdge(state, 12f)
    ) {
        items(suggestions) {
            FilledTonalButton(
                {
                    onSelect(it.date)
                }
            ) {
                Text(it.title)
            }
        }
    }
}
