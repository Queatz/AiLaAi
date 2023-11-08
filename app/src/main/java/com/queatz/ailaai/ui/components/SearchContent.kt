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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.helpers.LocationSelector
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun SearchContent(
    locationSelector: LocationSelector,
    isLoading: Boolean,
    categories: List<String>,
    category: String?,
    onCategory: (String?) -> Unit
) {
    if (locationSelector.isManual) {
        ElevatedButton(
            elevation = ButtonDefaults.elevatedButtonElevation(ElevationDefault * 2),
            onClick = {
                locationSelector.reset()
            }
        ) {
            Text(
                stringResource(R.string.reset_location),
                modifier = Modifier.padding(end = PaddingDefault)
            )
            Icon(Icons.Outlined.Clear, stringResource(R.string.reset_location))
        }
    }
    AnimatedVisibility (
        categories.size > 2 && !isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        var viewport by remember { mutableStateOf(Size(0f, 0f)) }
        val scrollState = rememberScrollState()
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
            modifier = Modifier
                .horizontalScroll(scrollState)
                .onPlaced { viewport = it.boundsInParent().size }
                .padding(horizontal = PaddingDefault * 2)
        ) {
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
                    elevation = ButtonDefaults.elevatedButtonElevation(ElevationDefault / 2),
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
