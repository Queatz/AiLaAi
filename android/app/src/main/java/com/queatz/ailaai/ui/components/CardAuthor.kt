package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person

@Composable
fun CardAuthor(
    people: List<Person>,
    interactable: Boolean,
    modifier: Modifier = Modifier
) {
    var viewport by rememberStateOf(Size(0f, 0f))
    val scrollState = rememberLazyListState()

    LazyRow(
        state = scrollState,
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier
            .fillMaxWidth()
            .onPlaced { viewport = it.boundsInParent().size }
            .horizontalFadingEdge(viewport, scrollState)
    ) {
        items(people) { person ->
            PersonItem(person, interactable)
        }
    }
}
