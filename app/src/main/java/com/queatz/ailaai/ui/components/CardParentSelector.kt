package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun CardParentSelector(
    value: CardParentType?,
    modifier: Modifier = Modifier,
    onChange: (CardParentType) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
        modifier = modifier
    ) {
        val colors = IconButtonDefaults.outlinedIconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primary)
        )
        OutlinedIconToggleButton(value == CardParentType.Person, {
            onChange(CardParentType.Person)
        }, colors = colors, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Person, stringResource(R.string.you))
        }
        OutlinedIconToggleButton(value == CardParentType.Map, {
            onChange(CardParentType.Map)
        }, colors = colors, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Place, stringResource(R.string.on_the_map))
        }
        OutlinedIconToggleButton(value == CardParentType.Card, {
            onChange(CardParentType.Card)
        }, colors = colors, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Search, stringResource(R.string.inside_another_card))
        }
    }
}
