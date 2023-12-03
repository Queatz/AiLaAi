package com.queatz.ailaai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.helpers.LocationSelector
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad

class SearchFilter(
    val name: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun SearchContent(
    locationSelector: LocationSelector,
    isLoading: Boolean,
    filters: List<SearchFilter> = emptyList(),
    categories: List<String>,
    category: String?,
    onCategory: (String?) -> Unit
) {
    if (locationSelector.isManual) {
        ElevatedButton(
            elevation = ButtonDefaults.elevatedButtonElevation(2.elevation),
            onClick = {
                locationSelector.reset()
            }
        ) {
            Text(
                stringResource(R.string.reset_location),
                modifier = Modifier.padding(end = 1.pad)
            )
            Icon(Icons.Outlined.Clear, stringResource(R.string.reset_location))
        }
    }
    AnimatedVisibility (
        (filters.isNotEmpty() || categories.size > 2) && !isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        var viewport by remember { mutableStateOf(Size(0f, 0f)) }
        val scrollState = rememberScrollState()
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.pad),
            modifier = Modifier
                .horizontalScroll(scrollState)
                .onPlaced { viewport = it.boundsInParent().size }
                .padding(horizontal = 2.pad)
        ) {
            filters.forEachIndexed { index, it ->
                Button(
                    {
                        it.onClick()
                    },
                    elevation = ButtonDefaults.elevatedButtonElevation(.5f.elevation),
                    colors = if (!it.selected) ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) else ButtonDefaults.buttonColors()
                ) {
                    Icon(Icons.Outlined.FilterList, null, modifier = Modifier.padding(end = 1.pad))
                    Text(it.name)
                }
            }
            categories.forEachIndexed { index, it ->
                Button(
                    {
                        onCategory(
                            if (category == it) {
                                null
                            } else {
                                it
                            }
                        )
                    },
                    elevation = ButtonDefaults.elevatedButtonElevation(.5f.elevation),
                    colors = if (category != it) ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) else ButtonDefaults.buttonColors()
                ) {
                    Text(it)
                }
            }
        }
    }
}
