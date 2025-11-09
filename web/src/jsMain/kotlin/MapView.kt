import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import app.ailaai.api.card
import app.ailaai.api.cards
import app.ailaai.api.newCard
import app.cards.MapList
import app.cards.mapListDialog
import app.components.Empty
import app.components.Spacer
import app.compose.rememberDarkMode
import app.dialog.inputDialog
import app.messaages.inList
import com.queatz.db.Card
import com.queatz.db.Geo
import com.queatz.db.formatPay
import components.CardContent
import components.Icon
import components.IconButton
import components.SearchField
import components.Switch
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.dom.addClass
import lib.ResizeObserver
import lib.getCameraLngLat
import lib.mapboxgl
import lib.mapboxgl.MarkerOptions
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundRepeat
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.lineHeight
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class CardMarker(
    val card: Card,
    val marker: mapboxgl.Marker
)

sealed interface SearchFilter {
    data object Paid : SearchFilter
}

@Composable
fun MapView(
    showList: Boolean = true,
    autoHideList: Boolean = false,
    onCardAdded: (Card) -> Unit = {},
    header: (@Composable () -> Unit)? = null,
) {
    var searchText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf(listOf<Card>()) }
    var geo by remember { mutableStateOf<Geo?>(null) }
    var map by remember { mutableStateOf<mapboxgl.Map?>(null) }
    var markers by remember { mutableStateOf(emptyList<CardMarker>()) }
    var selectedCard by remember { mutableStateOf<Card?>(null) }
    val scope = rememberCoroutineScope()
    val hasHash = remember { window.location.hash.isBlank() }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoriesCache by remember { mutableStateOf(emptyList<String>()) }
    var selectedFilters by remember { mutableStateOf(emptyList<SearchFilter>()) }
    var cardNavHistory by remember { mutableStateOf(listOf<Card>()) }
    val isDarkMode = rememberDarkMode()
    var currentStyleIndex by remember { mutableStateOf(0) }

    // Define map styles
    val mapStyles = listOf(
        "mapbox://styles/mapbox/standard",
        "mapbox://styles/mapbox/satellite-streets-v12",
        "mapbox://styles/mapbox/navigation-day-v1",
        "mapbox://styles/mapbox/navigation-night-v1"
    )

    val categories = remember(searchResults) {
        if (selectedCategory == null) {
            searchResults
                .mapNotNull { it.categories }
                .flatten()
                .sortedDistinct().also {
                    categoriesCache = it
                }
        } else {
            categoriesCache
        }

    }

    LaunchedEffect(selectedCard) {
        if (selectedCard == null) {
            cardNavHistory = emptyList()
        }
    }

    LaunchedEffect(categories) {
        if (selectedCategory !in categories) {
            selectedCategory = null
        }
    }

    LaunchedEffect(geo, searchText, selectedCategory) {
        geo ?: return@LaunchedEffect
        isLoading = true
        delay(250)

        val altitude = map!!.getFreeCameraOptions().position.toAltitude() as Double

        api.cards(
            geo = geo!!,
            altitude = altitude / 1000,
            search = searchText.notBlank ?: selectedCategory?.notBlank,
            public = true
        ) {
            // todo: filter geo != null on backend
            searchResults = it.filter { it.geo != null }
        }

        isLoading = false
    }

    fun update() {
        map ?: return

        fun mapboxgl.Point.near(other: mapboxgl.Point, distance: Int) =
            abs(x - other.x) <= distance && abs(y - other.y) <= distance

        val cameraLngLat = map!!.getCameraLngLat()
        val altitude = map!!.getFreeCameraOptions().position.toAltitude() as Double
        val nearDistance = 64
        val nearDistanceMax = 128
        val cardPositions = markers.mapIndexed { index, it ->
            index to map!!.project(it.marker.getLngLat())
        }

        markers.forEachIndexed { index, marker ->
            val pos = cardPositions[index].second
            val groundDistance = cameraLngLat.distanceTo(marker.marker.getLngLat())

            val nearScale = when {
                cardPositions.any {
                    it.first != index && markers[index].card < markers[it.first].card && markers[it.first].card.collides(markers[index].card) && it.second.near(pos, nearDistanceMax)
                } -> {
                    0f
                }
                cardPositions.any { it.first != index && markers[index].card < markers[it.first].card && it.second.near(pos, nearDistance) } -> {
                    0f
                }
                cardPositions.any { it.first != index && markers[index].card <= markers[it.first].card && it.second.near(pos, nearDistance / 4) } -> {
                    .25f
                }
                cardPositions.any { it.first != index && markers[index].card <= markers[it.first].card && it.second.near(pos, nearDistance) } -> {
                    .5f
                }
                else -> {
                    1f
                }
            }

            val scale = 100.0 / sqrt(groundDistance.pow(2.0) + altitude.pow(2.0))
            val element = marker.marker.getElement().firstElementChild as HTMLElement
            element.style.transform = "scale(${scale.coerceIn(0.125, 100.0) * nearScale})"
            marker.marker.getElement().style.zIndex = (scale * 1000.0).toInt().toString()
            val ele = marker.marker.getElement() // for the following line
            js("ele.style.pointerEvents = \"none\"")
        }

        // prevent mapbox from overriding pointer-events
        scope.launch {
            delay(100)
            markers.forEach {
                val ele = it.marker.getElement() // for the following line
                js("ele.style.pointerEvents = \"none\"")
            }
        }
    }

    val shownCards = remember(searchResults, selectedCategory, selectedFilters) {
        if (selectedCategory == null) {
            searchResults
        } else {
            searchResults.filter { it.categories?.contains(selectedCategory) == true }
        }.let {
            if (selectedFilters.isEmpty()) {
                it
            } else {
                it.filter { card ->
                    selectedFilters.all { filter ->
                        when (filter) {
                            is SearchFilter.Paid -> {
                                card.pay != null
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(shownCards, map) {
        map ?: return@LaunchedEffect

        markers.forEach {
            it.marker.remove()
        }

        markers = shownCards.map { card ->
            val element = document.createElement("div") as HTMLDivElement
            element.addClass(Styles.mapMarker)

            val options: MarkerOptions = js("{}")
            options.element = element
            options.anchor = "bottom"

            val latLng: mapboxgl.LngLat = js("{}")
            latLng.lat = card.geo!![0]
            latLng.lng = card.geo!![1]

            mapboxgl.Marker(options)
                .setLngLat(latLng)
                .addTo(map!!).also {
                    renderComposable(root = element) {
                        val isNpc = !card.npc?.photo.isNullOrBlank()

                        Div({
                            classes(Styles.mapMarkerContent)

                            onClick {
                                it.stopPropagation()

                                if (it.ctrlKey) {
                                    window.open("$webBaseUrl/page/${card.id!!}", target = "_blank")
                                } else {
                                    cardNavHistory = emptyList()
                                    selectedCard = if (selectedCard?.id == card.id) {
                                        null
                                    } else {
                                        card
                                    }
                                }
                            }
                        }) {
                            Div({
                                style {
                                    textAlign("center")
                                    fontSize(128.px)
                                    color(Styles.colors.black)
                                    padding(4.r)
                                    lineHeight(120.percent)

                                    if (isNpc) {
                                        property("box-shadow", "0 2px 16px rgba(0, 0, 0, 0.125)")
                                        borderRadius(6.r)
                                        backgroundColor(Styles.colors.white)
                                    }
                                }
                            }) {
                                if (isNpc) {
                                    Div({
                                        style {
                                            textAlign("left")
                                        }
                                    }) {
                                        B {
                                            Text(card.npc?.name.orEmpty())
                                        }
                                        Span({
                                            style {
                                                opacity(.5)
                                                paddingLeft(1.r)
                                                fontSize(85.percent)
                                            }
                                        }) {
                                            Text(card.name ?: application.appString { newCard })
                                        }
                                        card.npc?.text?.notBlank?.let {
                                            Br()
                                            Text(it)
                                        }
                                    }
                                } else {
                                    Div {
                                        Text(card.name ?: application.appString { newCard })
                                    }
                                    listOf(
                                        card.formatPay { appStringShort },
                                        card.categories?.firstOrNull()
                                    ).notEmpty?.let {
                                        Div({
                                            style {
                                                fontSize(85.percent)
                                                opacity(.5f)
                                            }
                                        }) {
                                            Text(bulletedString(*it.toTypedArray()))
                                        }
                                    }
                                }
                            }

                            Div({
                                style {
                                    backgroundRepeat("no-repeat")
                                    if (isNpc) {
                                        width(32.r)
                                        height(32.r)
                                        backgroundImage("url(\"$baseUrl${card.npc!!.photo}\")")
                                        backgroundPosition("center")
                                        backgroundSize("contain")
                                    } else if (!card.photo.isNullOrBlank()) {
                                        width(16.r)
                                        height(16.r)
                                        borderRadius(16.r)
                                        backgroundColor(Styles.colors.background)
                                        property("border", "${2.px} solid ${Styles.colors.white}")
                                        backgroundImage("url($baseUrl${card.photo!!})")
                                        backgroundPosition("center")
                                        backgroundSize("cover")
                                    } else {
                                        width(16.r)
                                        height(16.r)
                                        borderRadius(16.r)
                                        backgroundColor(Styles.colors.background)
                                        property("border", "${2.px} solid ${Styles.colors.white}")
                                    }
                                }
                            })
                        }
                    }
                }.let { marker ->
                    CardMarker(
                        card = card,
                        marker = marker
                    )
                }
        }

        update()
    }

    Div({
        style {
            property("inset", "0")
            position(Position.Absolute)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            property("z-index", "0")
        }

        ref { ref ->
            val locateMeControl = mapboxgl.GeolocateControl(
                js("{showAccuracyCircle: false}")
            )

            // Saigon
            val (initialZoom, initialLat, initialLng, initialBearing, initialPitch) = "13.27/10.77564/106.72394/-39.9/50".split("/")
            val options: mapboxgl.MapOptions = js("{}")
            val initialLngLat: mapboxgl.LngLat = js("{}")
            initialLngLat.lng = initialLng.toDouble()
            initialLngLat.lat = initialLat.toDouble()
            options.container = ref
            options.boxZoom = false
            options.hash = true
            options.zoom = initialZoom.toDouble()
            options.center = initialLngLat
            options.bearing = initialBearing.toDouble()
            options.pitch = initialPitch.toDouble()
            options.style = mapStyles[currentStyleIndex] // Use the current style

            val observer = ResizeObserver { _, _ ->
                map?.resize()
            }.apply {
                observe(ref)
            }

            map = mapboxgl.Map(options).apply {
                addControl(locateMeControl, "bottom-right")

                // Create style toggle button
                val styleToggleControl = object : mapboxgl.IControl {
                    private var button: HTMLElement? = null
                    private var iconElement: HTMLElement? = null

                    override fun onAdd(map: mapboxgl.Map): HTMLElement {
                        button = document.createElement("button") as HTMLElement
                        button?.className = "mapboxgl-ctrl-icon mapboxgl-ctrl-style-toggle"
                        button?.style?.display = "flex"
                        button?.style?.alignItems = "center"
                        button?.style?.justifyContent = "center"

                        // Create material icon
                        iconElement = document.createElement("span") as HTMLElement
                        iconElement?.className = "material-symbols-outlined"
                        iconElement?.textContent = "layers"
                        iconElement?.style?.fontSize = "24px"

                        button?.appendChild(iconElement!!)

                        button?.addEventListener("click", {
                            // Cycle to the next style
                            currentStyleIndex = (currentStyleIndex + 1) % mapStyles.size

                            // Apply the new style
                            map.setStyle(mapStyles[currentStyleIndex])
                        })

                        val container = document.createElement("div") as HTMLElement
                        container.className = "mapboxgl-ctrl mapboxgl-ctrl-group"
                        container.appendChild(button!!)
                        return container
                    }

                    override fun onRemove(map: mapboxgl.Map) {
                        button = null
                        iconElement = null
                    }
                }

                addControl(styleToggleControl, "bottom-left")

                on("load") {
                    geo = getCameraLngLat().let { Geo(it.lat, it.lng) }

                    if (hasHash) {
                        locateMeControl.trigger()
                    }
                }

                on("moveend") {
                    geo = if (getPitch() > 60.0) {
                        map!!.getCenter()
                    } else {
                        getCameraLngLat()
                    }.let { Geo(it.lat, it.lng) }
                }

                on("render") {
                    update()
                }

                on("click") {
                    val event = it as mapboxgl.MapMouseEvent
                    if (selectedCard != null) {
                        selectedCard = null
                    } else {
                        val click = event.lngLat
                        scope.launch {
                            var active = false
                            val cardName = inputDialog(
                                title = application.appString { newCard },
                                placeholder = application.appString { name },
                                confirmButton = application.appString { createCard },
                                content = { _, _, _ ->
                                    var value by remember { mutableStateOf(false) }

                                    LaunchedEffect(value) {
                                        active = value
                                    }

                                    Span({
                                        style {
                                            color(Styles.colors.gray)
                                            paddingTop(1f.r)
                                            paddingBottom(.5f.r)
                                        }
                                    }) {
                                        appText { publish }
                                    }

                                    Switch(
                                        value = value,
                                        onValue = {
                                            value = it
                                        },
                                        onChange = {
                                            value = it
                                        },
                                        border = true,
                                        title = if (value) {
                                            appString { publish }
                                        } else {
                                            appString { this.draft }
                                        }
                                    )
                                }
                            ) ?: return@launch

                            api.newCard(
                                Card(
                                    name = cardName,
                                    geo = listOf(click.lat, click.lng),
                                    active = active
                                )
                            ) {
                                // todo: just reload everything
                                searchResults = searchResults + it
                                onCardAdded(it)
                            }
                        }
                    }
                }
            }

            onDispose {
                map?.remove()
                map = null
                observer.disconnect()
            }
        }
    }) {}

    if (header == null) {
        Spacer()
    } else {
        header()
    }

    fun centerMapOnCard(card: Card?) {
        card?.geo?.toLatLng()?.let { lngLat ->
            val options: mapboxgl.FlyToOptions = js("{}")
            options.center = lngLat
            map?.flyTo(options)
        }
    }

    Div({
        classes(Styles.mapContainer)
    }) {
        if (showList) {
            Div({
                classes(Styles.navContainer, Styles.mapList)

                if (autoHideList) {
                    classes(Styles.mapListAutoHide)
                }
            }) {
                Div({
                    classes(Styles.navContent)
                }) {
                    var expanded by remember { mutableStateOf(true) }

                    Div({
                        style {
                            fontWeight("bold")
                            padding(1.r)
                            fontSize(24.px)
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.SpaceBetween)
                        }
                    }) {
                        Text("${appString { pages }} (${shownCards.size})")
                        IconButton(
                            name = if (expanded) "expand_less" else "expand_more",
                            title = if (expanded) appString { collapse } else appString { expand },
                            styles = {
                                property("border", "1px solid ${if (isDarkMode) Styles.colors.dark.outline else Styles.colors.outline}")
                                borderRadius(2.r)

                            }
                        ) {
                            expanded = !expanded
                        }
                    }
                    if (expanded) {
                        if (shownCards.isEmpty()) {
                            Empty { appText { noCardsNearby } }
                        } else {
                            MapList(shownCards) { card ->
                                cardNavHistory = emptyList()
                                selectedCard = if (selectedCard?.id == card.id) null else card
                                centerMapOnCard(selectedCard)
                            }
                        }
                    }
                }
            }
        }
        Div(
            {
                classes(Styles.mapUi)
            }
        ) {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    maxWidth(800.px)
                    width(100.percent)
                    alignSelf(AlignSelf.Center)
                    paddingLeft(1.r)
                    paddingRight(1.r)
                    boxSizing("border-box")
                }
            }) {
                SearchField(
                    value = searchText,
                    placeholder = appString { searchForPlaces },
                    shadow = true
                ) {
                    searchText = it
                }
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        gap(.5.r)
                        overflowY("auto")
                        paddingTop(.5.r)
                        paddingBottom(.5.r)
                        position(Position.Relative)
                    }
                }) {
                    if (searchResults.any { it.pay != null }) {
                        var paidFilterSelected = selectedFilters.any { it is SearchFilter.Paid }
                        Div({
                            classes(Styles.floatingButton)

                            if (paidFilterSelected) {
                                classes(Styles.floatingButtonSelected)
                            }

                            style {
                                flexShrink(0)
                            }

                            onClick {
                                selectedFilters = if (paidFilterSelected) {
                                    emptyList<SearchFilter>()
                                } else {
                                    SearchFilter.Paid.inList()
                                }
                            }
                        }) {
                            Icon("payments")
                            appText { paid }
                        }
                        if (categories.isNotEmpty()) {
                            Div({
                                style {
                                    margin(.5f.r)
                                    borderRadius(.5f.r)
                                    backgroundColor(Styles.colors.tertiary)
                                    flexShrink(0)
                                    width(.25.r)
                                    property("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.125)")
                                }
                            })
                        }
                    }
                    categories.forEach { category ->
                        Button({
                            classes(Styles.floatingButton)

                            if (selectedCategory == category) {
                                classes(Styles.floatingButtonSelected)
                            }

                            style {
                                flexShrink(0)
                            }

                            onClick {
                                selectedCategory = if (selectedCategory == category) null else category
                            }
                        }) {
                            Text(category)
                        }
                    }
                }
            }
        }
        if (selectedCard != null) {
            var panelRef by remember { mutableStateOf<HTMLElement?>(null) }

            LaunchedEffect(selectedCard) {
                panelRef?.scrollTop = 0.0
            }

            Div({
                classes(Styles.navContainer, Styles.mapPanel)

                ref { ref ->
                    panelRef = ref

                    onDispose {
                        panelRef = null
                    }
                }
            }) {
                Div({
                    classes(Styles.navContent)
                }) {
                    Div {
                        selectedCard?.let {
                            CardContent(
                                card = it,
                                onCardClick = { cardId, openInNewWindow ->
                                    if (openInNewWindow) {
                                        window.open("/page/$cardId", target = "_blank")
                                    } else {
                                        scope.launch {
                                            api.card(cardId) {
                                                selectedCard?.let {
                                                    cardNavHistory += it
                                                }
                                                selectedCard = it
                                                centerMapOnCard(it)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    cardNavHistory.lastOrNull()?.let { card ->
                        IconButton(
                            name = "arrow_back",
                            title = appString { goBack },
                            background = true,
                            styles = {
                                position(Position.Absolute)
                                top(1.r)
                                left(1.r)
                            }
                        ) {
                            cardNavHistory -= card
                            selectedCard = card
                            centerMapOnCard(card)
                        }
                }
                    Button({
                        classes(Styles.button)

                        style {
                            position(Position.Absolute)
                            top(1.r)
                            right(1.r)
                        }

                        onClick { event ->
                            window.open("/page/${selectedCard!!.id!!}", target = "_blank")
                        }
                    }) {
                        Span {
                            appText { openPage }
                        }
                        Span({
                            classes("material-symbols-outlined")
                            style {
                                paddingLeft(1.r)
                            }
                        }) {
                            Text("open_in_new")
                        }
                    }
                }
            }
        } else {
            Div({
                classes(Styles.mobileOnly)

                style {
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.Center)
                    property("z-index", "10")
                    alignSelf(AlignSelf.Stretch)
                    padding(2.r, 1.r)
                }
            }) {
                Button({
                    classes(Styles.button)

                    onClick {
                        scope.launch {
                            mapListDialog(shownCards) { card ->
                                window.open("/page/${card.id}", target = "_blank")
                            }
                        }
                    }
                }) {
                    Icon("list")
                    appText { viewList }
                }
            }
        }
    }
}

private fun List<Double>.toLatLng(): mapboxgl.LngLat? {
    return takeIf { size == 2 }?.let { array ->
        mapboxgl.LngLat.convert(array.asReversed().toTypedArray())
    }
}

private fun Card.collides(other: Card): Boolean {
    val geo = geo?.toLatLng() ?: return false
    val otherGeo = other.geo?.toLatLng() ?: return false

    return geo.distanceTo(otherGeo) < ((size ?: 0.0) + (other.size ?: 0.0)).kmToMeters
}

private val Double.kmToMeters get() = this * 1_000.0

private operator fun Card.compareTo(other: Card) = (level ?: 0).compareTo(other.level ?: 0).let { compareLevel ->
    if (compareLevel != 0) {
        compareLevel
    } else {
        (size ?: 0.0).compareTo(other.size ?: 0.0)
    }
}
