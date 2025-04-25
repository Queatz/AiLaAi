package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.ailaai.api.myCards
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.isFalse
import com.queatz.ailaai.extensions.isTrue
import com.queatz.ailaai.extensions.rememberAutoplayIndex
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.toggle
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.CardLayout
import com.queatz.ailaai.ui.components.CardParentSelector
import com.queatz.ailaai.ui.components.CardParentType
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.dialogs.EditCardDialog
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class Filter {
    Active,
    NotActive
}

private val meParentTypeKey = stringPreferencesKey("me.parentType")
private val meFiltersKey = stringSetPreferencesKey("me.filters")

// Card template data class
data class CardTemplate(
    val icon: ImageVector,
    val name: String,
    val description: String
)

@Composable
fun MeScreen() {
    var myCards by remember { mutableStateOf(emptyList<Card>()) }
    var isLoading by rememberStateOf(true)
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    var cardParentType by rememberSaveable { mutableStateOf<CardParentType?>(null) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var filters by rememberSaveable { mutableStateOf(emptySet<Filter>()) }
    val context = LocalContext.current
    var playingVideo by remember { mutableStateOf<Card?>(null) }
    val nav = nav

    // List of card templates
    val cardTemplates = remember {
        listOf(
            CardTemplate(Icons.Outlined.ShoppingBag, "Sell a product", "List an item for sale"),
            CardTemplate(Icons.Outlined.Handyman, "Provide a service", "Offer your skills and services"),
            CardTemplate(Icons.Outlined.Work, "Post a job", "Find someone for a job"),
            CardTemplate(Icons.Outlined.Event, "Create an event", "Organize a gathering or event"),
            CardTemplate(Icons.Outlined.Home, "Real estate", "List property for sale or rent"),
            CardTemplate(Icons.Outlined.DirectionsCar, "Vehicle", "Sell or rent a vehicle"),
            CardTemplate(Icons.Outlined.School, "Education", "Offer classes or tutoring"),
            CardTemplate(Icons.Outlined.Restaurant, "Food & Dining", "Share a recipe or food service"),
            CardTemplate(Icons.Outlined.Pets, "Pets", "Pet-related services or adoption"),
            CardTemplate(Icons.Outlined.HealthAndSafety, "Health & Wellness", "Health services or products"),
            CardTemplate(Icons.Outlined.Brush, "Art & Craft", "Showcase or sell artwork"),
            CardTemplate(Icons.Outlined.MusicNote, "Music", "Music lessons or performances"),
            CardTemplate(Icons.Outlined.SportsBasketball, "Sports", "Sports events or training"),
            CardTemplate(Icons.Outlined.Computer, "Technology", "Tech services or products"),
            CardTemplate(Icons.Outlined.LocalLibrary, "Books", "Book exchange or recommendations"),
            CardTemplate(Icons.Outlined.Apartment, "Community", "Community initiatives"),
            CardTemplate(Icons.Outlined.Favorite, "Volunteer", "Volunteer opportunities"),
            CardTemplate(Icons.Outlined.Diversity3, "Networking", "Professional networking"),
            CardTemplate(Icons.Outlined.Celebration, "Special Occasion", "Special occasion services"),
            CardTemplate(Icons.Outlined.EmojiEmotions, "Personal", "Personal announcements")
        )
    }

    LaunchedEffect(Unit) {
        context.dataStore.data.first().let {
            it[meParentTypeKey]?.let { parentType ->
                cardParentType = CardParentType.valueOf(parentType)
            }
            it[meFiltersKey]?.let { previousFilters ->
                filters = previousFilters.map { Filter.valueOf(it) }.toSet()
            }
        }
    }

    fun setParentType(newCardParentType: CardParentType?) {
        cardParentType = newCardParentType
        scope.launch {
            context.dataStore.edit {
                if (newCardParentType == null) {
                    it.remove(meParentTypeKey)
                } else {
                    it[meParentTypeKey] = newCardParentType.name
                }
            }
        }
    }

    fun setFilters(newFilters: Set<Filter>) {
        filters = newFilters
        scope.launch {
            context.dataStore.edit {
                if (cardParentType == null) {
                    it.remove(meFiltersKey)
                } else {
                    it[meFiltersKey] = newFilters.map { it.name }.toSet()
                }
            }
        }
    }

    suspend fun reload() {
        api.myCards {
            myCards = it
        }
    }

    LaunchedEffect(Unit) {
        reload()
        isLoading = false
    }

    val cards = remember(myCards, cardParentType, filters, searchText) {
        when (cardParentType) {
            CardParentType.Person -> myCards.filter { it.parent == null && it.equipped.isTrue }
            CardParentType.Map -> myCards.filter { it.parent == null && it.equipped.isFalse && it.geo != null && it.offline.isFalse }
            CardParentType.Card -> myCards.filter { it.parent != null }
            CardParentType.Group -> myCards.filter { it.group != null }
            CardParentType.Offline -> myCards.filter { it.offline == true }
            else -> myCards
        }.filter {
            val searchTextTrimmed = searchText.trim()
            filters.all { filter ->
                when (filter) {
                    Filter.Active -> it.active == true
                    Filter.NotActive -> it.active != true
                }
            } &&
                    (searchTextTrimmed.isBlank() || (
                            it.conversation?.contains(searchTextTrimmed, true) == true ||
                                    it.name?.contains(searchTextTrimmed, true) == true ||
                                    it.location?.contains(searchTextTrimmed, true) == true
                            ))
        }
    }

    var newCard by rememberStateOf<Card?>(null)

    if (newCard != null) {
        EditCardDialog(
            newCard!!,
            {
                newCard = null
            },
            create = true
        ) {
            nav.appNavigate(AppNav.Page(it.id!!))
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AppHeader(
            stringResource(
                when (cardParentType) {
                    CardParentType.Map -> R.string.at_a_location
                    CardParentType.Card -> R.string.inside_another_card
                    CardParentType.Group -> R.string.in_a_group
                    CardParentType.Person -> R.string.on_profile
                    CardParentType.Offline -> R.string.none
                    else -> R.string.your_cards
                }
            ) + (if (isLoading) "" else " (${cards.size})"),
            {
                scope.launch {
                    state.scrollToTop()
                }
            }
        )

        // Card Template Selector
        var templateViewport by remember { mutableStateOf(Size(0f, 0f)) }
        val templateListState = rememberLazyListState()

        LazyRow(
            state = templateListState,
            contentPadding = PaddingValues(horizontal = 1.pad),
            horizontalArrangement = Arrangement.spacedBy(1.pad),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.pad)
                .onPlaced { templateViewport = it.boundsInParent().size }
                .horizontalFadingEdge(templateViewport, templateListState)
        ) {
            items(cardTemplates) { template ->
                CardTemplateItem(
                    template = template,
                    onClick = {
                        // Create a new card with the template name
                        val card = Card()
                        card.name = template.name
                        newCard = card
                    }
                )
            }
        }
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading) {
                Loading()
            } else {
                val autoplayIndex by state.rememberAutoplayIndex()
                LaunchedEffect(autoplayIndex) {
                    playingVideo = cards.getOrNull(autoplayIndex)
                }
                LazyVerticalGrid(
                    state = state,
                    contentPadding = PaddingValues(
                        1.pad,
                        1.pad,
                        1.pad,
                        1.pad + 80.dp + 58.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.Top),
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(240.dp)
                ) {
                    items(cards, key = { it.id!! }) { card ->
                        CardLayout(
                            card = card,
                            showTitle = true,
                            onClick = {
                                nav.appNavigate(AppNav.Page(card.id!!))
                            },
                            scope = scope,
                            playVideo = playingVideo == card
                        )
                    }

                    if (cards.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                stringResource(if (cardParentType == null && filters.isEmpty() && searchText.isBlank()) R.string.you_have_no_cards else R.string.no_cards_to_show),
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(2.pad)
                            )
                        }
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier
                    .padding(vertical = 2.pad)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
            ) {
                var viewport by remember { mutableStateOf(Size(0f, 0f)) }
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .onPlaced { viewport = it.boundsInParent().size }
                        .padding(horizontal = 1.pad)
                ) {
                    CardParentSelector(
                        cardParentType,
                        modifier = Modifier
                            .wrapContentWidth(unbounded = true)
                            .padding(horizontal = 1.pad)
                            .padding(bottom = .5f.pad),
                        showOffline = true
                    ) {
                        setParentType(if (it == cardParentType) null else it)
                    }
                    Button(
                        {
                            setFilters(filters.minus(Filter.NotActive).toggle(Filter.Active))
                        },
                              elevation = ButtonDefaults.elevatedButtonElevation(.5f.elevation),
                        colors = if (!filters.contains(Filter.Active)) ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ) else ButtonDefaults.buttonColors(),
                        modifier = Modifier.padding(end = 1.pad)
                    ) {
                        Text(stringResource(R.string.posted))
                    }
                    Button(
                        {
                            setFilters(filters.minus(Filter.Active).toggle(Filter.NotActive))
                        },
                               elevation = ButtonDefaults.elevatedButtonElevation(.5f.elevation),
                        colors = if (!filters.contains(Filter.NotActive)) ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ) else ButtonDefaults.buttonColors(),
                        modifier = Modifier.padding(end = 1.pad)
                    ) {
                        Text(stringResource(R.string.not_posted))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 2.pad)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentWidth()
                    ) {
                        SearchField(
                            searchText,
                            { searchText = it }
                        )
                    }
                    FloatingActionButton(
                        onClick = {
                            newCard = Card()
                        },
                        modifier = Modifier
                            .padding(start = 2.pad)
                    ) {
                        Icon(Icons.Outlined.Add, stringResource(R.string.add_a_card))
                    }
                }
            }
        }
    }
}

@Composable
fun CardTemplateItem(
    template: CardTemplate,
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
            Icon(
                template.icon,
                contentDescription = template.name,
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 0.5f.pad)
            )
            Text(
                template.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Text(
                template.description,
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
