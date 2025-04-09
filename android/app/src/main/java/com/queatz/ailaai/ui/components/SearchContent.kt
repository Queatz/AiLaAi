package com.queatz.ailaai.ui.screens

import android.R.attr.category
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.helpers.LocationSelector
import com.queatz.ailaai.ui.components.Categories
import com.queatz.ailaai.ui.components.SearchFilter
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad

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
    Categories(
        categories = categories,
        category = category,
        filters = filters,
        visible = category != null || (filters.isNotEmpty() || categories.size > 2) && !isLoading,
        onCategory = onCategory
    )
}
