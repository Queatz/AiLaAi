package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.pad

@Composable
fun CardParentSelector(
    value: CardParentType?,
    modifier: Modifier = Modifier,
    showOffline: Boolean = false,
    onChange: (CardParentType) -> Unit
) {
    // todo: make this a loop
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.pad),
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
            elevation = ButtonDefaults.elevatedButtonElevation(.5f.pad)
        ) {
            Icon(Icons.Outlined.Person, stringResource(R.string.on_profile))
            Text(stringResource(R.string.on_profile), modifier = Modifier.padding(start = 1.pad))
        }
        Button(
            {
                onChange(CardParentType.Map)
            },
            colors = if (value == CardParentType.Map) checkedColors else colors,
            elevation = ButtonDefaults.elevatedButtonElevation(.5f.pad)
        ) {
            Icon(Icons.Outlined.Place, stringResource(R.string.at_a_location))
            Text(stringResource(R.string.at_a_location), modifier = Modifier.padding(start = 1.pad))
        }
        Button(
            {
                onChange(CardParentType.Card)
            },
            colors = if (value == CardParentType.Card) checkedColors else colors,
            elevation = ButtonDefaults.elevatedButtonElevation(.5f.pad)
        ) {
            Icon(Icons.Outlined.Description, stringResource(R.string.inside_another_card))
            Text(stringResource(R.string.inside_another_card), modifier = Modifier.padding(start = 1.pad))
        }
        Button(
            {
                onChange(CardParentType.Group)
            },
            colors = if (value == CardParentType.Group) checkedColors else colors,
            elevation = ButtonDefaults.elevatedButtonElevation(.5f.pad)
        ) {
            Icon(Icons.Outlined.Group, stringResource(R.string.in_a_group))
            Text(stringResource(R.string.in_a_group), modifier = Modifier.padding(start = 1.pad))
        }

        if (showOffline) {
            Button(
                {
                    onChange(CardParentType.Offline)
                },
                colors = if (value == CardParentType.Offline) checkedColors else colors,
                elevation = ButtonDefaults.elevatedButtonElevation(.5f.pad)
            ) {
                Icon(Icons.Outlined.LocationOff, stringResource(R.string.none))
                Text(stringResource(R.string.none), modifier = Modifier.padding(start = 1.pad))
            }
        }
    }
}
