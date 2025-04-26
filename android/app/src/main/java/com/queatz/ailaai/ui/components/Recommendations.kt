package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.ui.theme.pad

data class Recommendation<T>(
    val value: T,
    val name: String,
    val icon: ImageVector? = null,
    val description: String = ""
)

@Composable
fun <T> Recommendations(
    items: List<Recommendation<T>>,
    modifier: Modifier = Modifier,
    onRecommendationClick: (T) -> Unit
) {
    var viewport by remember { mutableStateOf(Size(0f, 0f)) }
    val state = rememberLazyListState()

    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 1.pad),
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .onPlaced { viewport = it.boundsInParent().size }
            .horizontalFadingEdge(viewport, state)
    ) {
        items(items) { items ->
            RecommendationItem(
                item = items,
                onClick = {
                    onRecommendationClick(items.value)
                }
            )
        }
    }
}

@Composable
fun <T> RecommendationItem(
    item: Recommendation<T>,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .width(180.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(1.pad)
                .fillMaxWidth()
        ) {

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
            )
            item.icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 0.5f.pad)
                )
            }
            item.description.notBlank?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 0.25f.pad)
                        .fillMaxWidth()
                )
            }
        }
    }
}
