package com.queatz.ailaai.ui.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.ailaai.api.cards
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.distance
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.extensions.toLatLng
import com.queatz.db.Card
import io.ktor.utils.io.CancellationException

data class MapCardsControl(
    val cards: List<Card>,
    val areaCard: Card?,
    val loadMore: suspend (reload: Boolean) -> Unit,
    val mapCategoriesControl: MapCategoriesControl,
    val isLoading: Boolean,
    val isError: Boolean,
    val hasMore: Boolean,
)

private var cache = listOf<Card>()

@Composable
fun mapCardsControl(
    geo: LatLng?,
    altitude: Double?,
    filterPaid: Boolean,
    value: String,
    onLoadNewPage: (geo: LatLng?, value: String, clear: Boolean) -> Unit
): MapCardsControl {
    var cards by remember { mutableStateOf(cache) }
    var isError by rememberStateOf(false)
    var offset by remember { mutableIntStateOf(0) }
    var hasMore by rememberStateOf(true)
    var isLoading by rememberStateOf(cards.isEmpty())

    val mapCategoriesControl = mapCategoriesControl(cards = cards)

    LaunchedEffect(cards) {
        cache = cards
    }

    val areaCard = remember(mapCategoriesControl.cardsOfCategory, geo) {
        if (geo == null) return@remember null
        // todo: translate
        mapCategoriesControl.cardsOfCategory
            .filter { page ->
                page.geo?.toLatLng()?.let { geo ->
                    geo.distance(geo) < (page.size ?: 0.0) * 1000
                } == true && !page.name.isNullOrBlank()
            }.maxByOrNull { it.size ?: 0.0 }
    }

    fun onNewPage(page: List<Card>, clear: Boolean) {
        val oldSize = if (clear) 0 else cards.size
        cards = if (clear) {
            page
        } else {
            (cards + page).distinctBy { it.id }
        }

        offset = cards.size
        hasMore = cards.size > oldSize
        isError = false

        mapCategoriesControl.updateCategories()
        isLoading = false
        onLoadNewPage(geo, value, clear)
    }

    suspend fun loadMore(
        reload: Boolean = false,
    ) {
        val geo = geo ?: geo ?: return
        if (reload) {
            offset = 0
            hasMore = true
        }
        api.cards(
            geo = geo.toGeo(),
            altitude = (altitude ?: 0.0) / 1000.0,
            offset = offset,
            paid = filterPaid.takeIf { it },
            search = value.notBlank ?: mapCategoriesControl.selectedCategory,
            public = true,
            onError = { ex ->
                if (ex is CancellationException || ex is InterruptedException) {
                    // Ignore, probably geo or search value changed, keep isLoading = true
                } else {
                    isLoading = false
                    isError = true
                }
            }
        ) {
            onNewPage(it, reload)
        }
    }

    return MapCardsControl(
        cards = cards,
        areaCard = areaCard,
        loadMore = { loadMore(it) },
        mapCategoriesControl = mapCategoriesControl,
        isLoading = isLoading,
        isError = isError,
        hasMore = hasMore
    )
}
