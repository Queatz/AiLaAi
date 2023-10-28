package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
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
    showOffline: Boolean = false,
    onChange: (CardParentType) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
        modifier = modifier
    ) {
        val colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background)
        )
        val checkedColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primary)
        )
        Button(
            {
                onChange(CardParentType.Person)
            },
            colors = if (value == CardParentType.Person) checkedColors else colors,
            elevation = ButtonDefaults.elevatedButtonElevation(PaddingDefault / 2),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Outlined.Person, stringResource(R.string.on_profile))
        }
        Button(
            {
                onChange(CardParentType.Map)
            },
            colors = if (value == CardParentType.Map) checkedColors else colors,
            elevation = ButtonDefaults.elevatedButtonElevation(PaddingDefault / 2),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Outlined.Place, stringResource(R.string.at_a_location))
        }
        if (showOffline) {
            Button(
                {
                    onChange(CardParentType.Offline)
                },
                colors = if (value == CardParentType.Offline) checkedColors else colors,
                elevation = ButtonDefaults.elevatedButtonElevation(PaddingDefault / 2),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.CloudOff, stringResource(R.string.none))
            }
        } else {
            Button(
                {
                    onChange(CardParentType.Card)
                },
                colors = if (value == CardParentType.Card) checkedColors else colors,
                elevation = ButtonDefaults.elevatedButtonElevation(PaddingDefault / 2),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.Search, stringResource(R.string.inside_another_card))
            }
        }
    }
}
