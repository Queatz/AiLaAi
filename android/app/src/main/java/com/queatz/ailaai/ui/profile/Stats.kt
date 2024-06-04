package com.queatz.ailaai.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.dayMonthYear
import com.queatz.ailaai.extensions.monthYear
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import com.queatz.db.ProfileStats

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Stats(stats: ProfileStats, person: Person?) {
    var showJoined by rememberStateOf(false)

    if (showJoined) {
        AlertDialog(
            {
                showJoined = false
            },
            title = {
                Text(stringResource(R.string.joined))
            },
            text = {
                Text(person?.createdAt?.dayMonthYear() ?: "?")
            },
            confirmButton = {
                TextButton(
                    {
                        showJoined = false
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(
            2.pad,
            Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(2.pad, Alignment.CenterVertically),
        modifier = Modifier
            .padding(2.pad)
            .fillMaxWidth()
    ) {
        StatsCard(
            title = stats.friendsCount.toString(),
            text = pluralStringResource(R.plurals.friends_plural, stats.friendsCount, stats.friendsCount)
        )
        StatsCard(
            title = stats.cardCount.toString(),
            text = pluralStringResource(R.plurals.cards_plural, stats.cardCount, stats.cardCount)
        )
        StatsCard(
            title = person?.createdAt?.monthYear() ?: "?",
            text = stringResource(R.string.joined),
            onClick = { showJoined = true }
        )
        StatsCard(
            title = stats.storiesCount.toString(),
            text = pluralStringResource(R.plurals.stories_plural, stats.storiesCount, stats.storiesCount)
        )
        StatsCard(
            title = stats.subscriberCount.toString(),
            text = pluralStringResource(R.plurals.subscribers_plural, stats.subscriberCount, stats.subscriberCount)
        )
    }
}

@Composable
fun RowScope.StatsCard(title: String, text: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .weight(1f)
            .widthIn(min = 64.dp)
            .padding(1.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
