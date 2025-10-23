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
import com.queatz.ailaai.extensions.at
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.plus
import com.queatz.ailaai.ui.theme.pad
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

data class DateTimeSuggestion(
    val title: String,
    val date: Instant
)

@Composable
fun DateTimeSuggestions(
    modifier: Modifier = Modifier,
    onSelect: (Instant) -> Unit
) {
    val state = rememberLazyListState()
    val now = Clock.System.now()
    val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())

    // Create a list to hold all suggestions
    val suggestionsList = mutableListOf(
        DateTimeSuggestion(stringResource(R.string.now), now),
        DateTimeSuggestion(stringResource(R.string.in_an_hour), now + 1.hours),
        DateTimeSuggestion(stringResource(R.string.in_2_days), now + 2.days),
        DateTimeSuggestion(stringResource(R.string.in_a_week), now + 7.days),
        DateTimeSuggestion(stringResource(R.string.in_2_weeks), now + 14.days),
        DateTimeSuggestion(stringResource(R.string.in_a_month), now + 30.days),
        DateTimeSuggestion(stringResource(R.string.in_2_months), now + 60.days),
        DateTimeSuggestion(stringResource(R.string.in_a_year), now + 365.days)
    )

    // Add "In 2 hours" suggestion
    suggestionsList.add(
        DateTimeSuggestion(stringResource(R.string.in_2_hours), now + 2.hours)
    )

    // Add "Tonight at 7pm" suggestion if it's before 7pm today
    if (localDateTime.hour < 19) {
        val tonightAt7pm = now.at(hour = 19)
        suggestionsList.add(
            DateTimeSuggestion(stringResource(R.string.tonight_at_7pm), tonightAt7pm)
        )
    }

    // Add "In the morning at 7am" suggestion if it's after 7am today (for tomorrow morning)
    if (localDateTime.hour >= 7) {
        val tomorrowMorningAt7am = now.plus(days = 1).at(hour = 7)
        suggestionsList.add(
            DateTimeSuggestion(stringResource(R.string.morning_at_7am), tomorrowMorningAt7am)
        )
    }

    // Add "Tomorrow night at 7pm" suggestion
    val tomorrowNightAt7pm = now.plus(days = 1).at(hour = 19)
    suggestionsList.add(
        DateTimeSuggestion(stringResource(R.string.tomorrow_night_at_7pm), tomorrowNightAt7pm)
    )

    // Add "This weekend at 7am" suggestion if it's not already the weekend
    if (localDateTime.dayOfWeek.value < 6) { // If today is not Saturday or Sunday
        val daysUntilWeekend = 6 - localDateTime.dayOfWeek.value // Days until Saturday
        val weekendMorningAt7am = now.plus(days = daysUntilWeekend).at(hour = 7)
        suggestionsList.add(
            DateTimeSuggestion(stringResource(R.string.weekend_at_7am), weekendMorningAt7am)
        )
    }

    // Add "Tomorrow at 7am" suggestion
    val tomorrowAt7am = now.plus(days = 1).at(hour = 7)
    suggestionsList.add(
        DateTimeSuggestion(stringResource(R.string.tomorrow_at_7am), tomorrowAt7am)
    )

    // Add "In 10 years" suggestion
    val inTenYears = now.plus(years = 10)
    suggestionsList.add(
        DateTimeSuggestion(stringResource(R.string.in_10_years), inTenYears)
    )

    // Sort suggestions by how far away they are from now
    val suggestions = suggestionsList.sortedBy { it.date }

    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier
            .fillMaxWidth()
            .horizontalFadingEdge(state, 12f)
    ) {
        items(suggestions) {
            FilledTonalButton(
                onClick = {
                    onSelect(it.date)
                }
            ) {
                Text(it.title)
            }
        }
    }
}
