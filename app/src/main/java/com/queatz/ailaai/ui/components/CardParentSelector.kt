package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun CardParentSelector(value: CardParentType, onChange: (CardParentType) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PaddingDefault)
    ) {
        OutlinedIconToggleButton(value == CardParentType.Person, {
            onChange(CardParentType.Person)
        }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Person, stringResource(R.string.you))
        }
        OutlinedIconToggleButton(value == CardParentType.Map, {
            onChange(CardParentType.Map)
        }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Place, stringResource(R.string.on_the_map))
        }
        OutlinedIconToggleButton(value == CardParentType.Card, {
            onChange(CardParentType.Card)
        }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Search, stringResource(R.string.inside_another_card))
        }
    }
}
