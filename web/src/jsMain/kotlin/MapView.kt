import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.AppStyles
import app.ailaai.api.card
import app.ailaai.api.cards
import app.ailaai.api.newCard
import app.cards.MapList
import app.components.BottomSheet
import app.components.BottomSheetState
import app.components.Empty
import app.components.FlexInput
import app.components.Spacer
import app.compose.rememberMobileMode
import app.dialog.inputDialog
import com.queatz.db.Card
import com.queatz.db.Geo
import com.queatz.db.Parking
import components.CardContent
import components.IconButton
import components.Markdown
import components.Switch
import components.activityDescription
import components.activityTime
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.dom.addClass
import lib.ResizeObserver
import lib.getCameraLngLat
import lib.mapboxCss
import lib.mapboxgl
import lib.mapboxgl.MarkerOptions
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
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
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class CardMarker(
    val card: Card,
    val marker: mapboxgl.Marker
)


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
    var cardNavHistory by remember { mutableStateOf(listOf<Card>()) }
    val isMobile = rememberMobileMode()
    var bottomSheetState by remember { mutableStateOf(BottomSheetState.Collapsed) }
    var currentStyleIndex by remember { mutableStateOf(0) }
    var availableNowFilter by remember { mutableStateOf(localStorage["map.availableNowFilter"]?.toBoolean() ?: true) }
    var petsFilter by remember { mutableStateOf(localStorage["map.petsFilter"]?.toBoolean() ?: false) }
    var outdoorsFilter by remember { mutableStateOf(localStorage["map.outdoorsFilter"]?.toBoolean() ?: false) }
    var selectedLanguages by remember { mutableStateOf<List<String>>(localStorage["map.selectedLanguages"]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()) }
    var ageMin by remember { mutableStateOf<Int?>(localStorage["map.ageMin"]?.toIntOrNull()) }
    var ageMax by remember { mutableStateOf<Int?>(localStorage["map.ageMax"]?.toIntOrNull()) }
    var groupSizeMin by remember { mutableStateOf<Int?>(localStorage["map.groupSizeMin"]?.toIntOrNull()) }
    var groupSizeMax by remember { mutableStateOf<Int?>(localStorage["map.groupSizeMax"]?.toIntOrNull()) }
    var parkingFilter by remember { mutableStateOf<Parking?>(localStorage["map.parkingFilter"]?.let { runCatching { Parking.valueOf(it) }.getOrNull() }) }
    var filtersExpanded by remember { mutableStateOf(true) }

    val allLanguages = remember(searchResults) {
        searchResults
            .flatMap { it.activity?.languages ?: emptyList() }
            .distinct()
            .sorted()
    }

    // Define map styles
    val mapStyles = listOf(
        "mapbox://styles/mapbox/standard",
        "mapbox://styles/mapbox/satellite-streets-v12",
        "mapbox://styles/mapbox/navigation-day-v1",
        "mapbox://styles/mapbox/navigation-night-v1"
    )

    var expanded by remember { mutableStateOf(true) }

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

        if (selectedCard != null && isMobile) {
            bottomSheetState = BottomSheetState.Half
        }
    }

    LaunchedEffect(categories) {
        if (selectedCategory !in categories) {
            selectedCategory = null
        }
    }

    LaunchedEffect(availableNowFilter) {
        localStorage["map.availableNowFilter"] = availableNowFilter.toString()
    }

    LaunchedEffect(petsFilter) {
        localStorage["map.petsFilter"] = petsFilter.toString()
    }

    LaunchedEffect(outdoorsFilter) {
        localStorage["map.outdoorsFilter"] = outdoorsFilter.toString()
    }

    LaunchedEffect(selectedLanguages) {
        localStorage["map.selectedLanguages"] = selectedLanguages.joinToString(",")
    }

    LaunchedEffect(ageMin) {
        if (ageMin != null) localStorage["map.ageMin"] = ageMin.toString()
        else localStorage.removeItem("map.ageMin")
    }

    LaunchedEffect(ageMax) {
        if (ageMax != null) localStorage["map.ageMax"] = ageMax.toString()
        else localStorage.removeItem("map.ageMax")
    }

    LaunchedEffect(groupSizeMin) {
        if (groupSizeMin != null) localStorage["map.groupSizeMin"] = groupSizeMin.toString()
        else localStorage.removeItem("map.groupSizeMin")
    }

    LaunchedEffect(groupSizeMax) {
        if (groupSizeMax != null) localStorage["map.groupSizeMax"] = groupSizeMax.toString()
        else localStorage.removeItem("map.groupSizeMax")
    }

    LaunchedEffect(parkingFilter) {
        if (parkingFilter != null) localStorage["map.parkingFilter"] = parkingFilter!!.name
        else localStorage.removeItem("map.parkingFilter")
    }

    LaunchedEffect(
        geo,
        searchText,
        selectedCategory,
        availableNowFilter,
        petsFilter,
        outdoorsFilter,
        selectedLanguages,
        ageMin,
        ageMax,
        groupSizeMin,
        groupSizeMax,
        parkingFilter
    ) {
        geo ?: return@LaunchedEffect
        isLoading = true
        delay(250)

        val altitude = map!!.getFreeCameraOptions().position.toAltitude() as Double

        api.cards(
            geo = geo!!,
            altitude = altitude / 1000,
            search = searchText.notBlank ?: selectedCategory?.notBlank,
            public = true,
            availableNow = availableNowFilter.takeIf { it }, // null means both
            pets = petsFilter.takeIf { it }, // null means both
            outdoors = outdoorsFilter.takeIf { it }, // null means both
            languages = selectedLanguages.takeIf { it.isNotEmpty() },
            minAge = ageMin,
            maxAge = ageMax,
            minGroupSize = groupSizeMin,
            maxGroupSize = groupSizeMax,
            activityActive = true,
            parking = parkingFilter
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

        val rawScales = markers.map { marker ->
            val groundDistance = cameraLngLat.distanceTo(
                marker.marker.getLngLat()
            )
            100.0 / sqrt(
                groundDistance.pow(2.0) + altitude.pow(2.0)
            )
        }

        val zIndices = rawScales
            .mapIndexed { index, scale ->
                index to scale
            }
            .sortedWith(
                compareByDescending<Pair<Int, Double>> { cardPositions[it.first].second.y }
                    .thenByDescending { it.second }
            )
            .mapIndexed { rank, pair ->
                pair.first to (markers.size - rank)
            }
            .toMap()

        markers.forEachIndexed { index, marker ->
            val pos = cardPositions[index].second

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

            val rawScale = rawScales[index]
            val scale = rawScale.coerceIn(0.125, 1.0)
            val contentElement = marker.marker.getElement().firstElementChild as? HTMLElement ?: return@forEachIndexed
            val innerElement = contentElement.firstElementChild as? HTMLElement ?: return@forEachIndexed
            val totalScale = scale * nearScale
            contentElement.style.transform = "scale($scale)"
            innerElement.style.transform = "scale($nearScale)"

            if (totalScale > 0) {
                contentElement.style.maxWidth = "calc(100vw / ${totalScale * 1.5f})"
            }
            marker.marker.getElement().style.zIndex = zIndices.getValue(index).toString()
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

    val shownCards = remember(searchResults, selectedCategory) {
        if (selectedCategory == null) {
            searchResults
        } else {
            searchResults.filter { it.categories?.contains(selectedCategory) == true }
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
                        }) {
                            Div({
                                classes(Styles.mapMarkerInner)

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
                                    classes(Styles.mapMarkerBox)
                                }) {
                                if (isNpc) {
                                    Div {
                                        Div({
                                            style {
                                                fontWeight("bold")
                                            }
                                        }) {
                                            Text(card.npc?.name.orEmpty())
                                        }
                                        Div({
                                            style {
                                                opacity(.5)
                                                paddingLeft(1.r)
                                                fontSize(85.percent)
                                            }
                                        }) {
                                            Text(card.name ?: application.appString { newCard })
                                        }
                                    }
                                    card.npc?.text?.notBlank?.let {
                                        Markdown(it)
                                    }
                                } else {
                                    Div({
                                        style {
                                            fontWeight("bold")
                                        }
                                    }) {
                                        Text(card.name ?: application.appString { newCard })
                                    }
                                    card.activity?.let { activity ->
                                        Div({
                                            style {
                                                color(Styles.colors.primary)
                                                fontWeight("bold")
                                                fontSize(85.percent)
                                            }
                                        }) {
                                            Text(activityTime(activity))
                                        }
                                    }
                                    card.activityDescription(includeTime = false, full = false).notBlank?.let { desc ->
                                        Div({
                                            style {
                                                fontSize(85.percent)
                                            }
                                        }) {
                                            Text(desc)
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
            // Ensure Mapbox CSS is bundled (ESM build, no global require)
            val __mapboxCss = mapboxCss

            mapboxgl.accessToken =
                "pk.eyJ1IjoiamFjb2JmZXJyZXJvIiwiYSI6ImNraXdyY211eTBlMmcycW02eDNubWNpZzcifQ.1KtSoMzrPCM0A8UVtI_gdg"

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
                    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                    val event = it.asDynamic() as mapboxgl.MapMouseEvent
                    if (selectedCard != null) {
                        selectedCard = null
                    } else if (application.me.value != null) {
                        val click = event.lngLat
                        scope.launch {
                            var active = false
                            var configuredActivity by mutableStateOf<com.queatz.db.Activity?>(null)
                            val cardName = inputDialog(
                                title = application.appString { newCard },
                                placeholder = application.appString { name },
                                confirmButton = application.appString { createCard },
                                content = { _, _, _ ->
                                    val contentScope = rememberCoroutineScope()
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

                                    Button({
                                        classes(Styles.outlineButton)
                                        style {
                                            width(100.percent)
                                            marginTop(1.r)
                                        }
                                        onClick {
                                            contentScope.launch {
                                                configuredActivity = app.dialog.configureActivityDialog(
                                                    initial = configuredActivity
                                                )
                                            }
                                        }
                                    }) {
                                        Text(
                                            if (configuredActivity != null) {
                                                Card(activity = configuredActivity).activityDescription(full = false).takeIf { it.isNotBlank() }
                                                    ?: appString { editActivity }
                                            } else {
                                                appString { addActivity }
                                            }
                                        )
                                    }
                                }
                            ) ?: return@launch

                            api.newCard(
                                Card(
                                    name = cardName,
                                    geo = listOf(click.lat, click.lng),
                                    active = active,
                                    activity = configuredActivity
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
            options.zoom = 16.0
            map?.flyTo(options)
        }
    }

    Div({
        classes(Styles.mapContainer)
    }) {
        if (!isMobile && (showList || selectedCard != null)) {
            var panelRef by remember { mutableStateOf<HTMLElement?>(null) }

            LaunchedEffect(selectedCard) {
                panelRef?.scrollTop = 0.0
            }

            Div({
                classes(Styles.navContainer)
                if (selectedCard != null) {
                    classes(Styles.mapPanel)
                } else {
                    classes(Styles.mapList)
                }

                if (autoHideList && selectedCard == null) {
                    classes(Styles.mapListAutoHide)
                }

                if (selectedCard != null) {
                    ref { ref ->
                        panelRef = ref

                        onDispose {
                            panelRef = null
                        }
                    }
                }
            }) {
                Div({
                    classes(Styles.navContent)
                }) {
                    if (selectedCard == null) {
                        Div({
                            classes(Styles.stickyHeader)
                            style {
                                fontWeight("bold")
                                padding(1.r)
                                fontSize(24.px)
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                justifyContent(JustifyContent.SpaceBetween)
                            }
                        }) {
                            Text(appString { explore })
                            IconButton(
                                name = if (expanded) "expand_less" else "expand_more",
                                title = if (expanded) appString { collapse } else appString { expand },
                                styles = {
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
                                MapList(
                                    cards = shownCards,
                                    styles = {
                                        padding(1.r)
                                    }
                                ) { card ->
                                    cardNavHistory = emptyList()
                                    selectedCard = if (selectedCard?.id == card.id) null else card
                                    centerMapOnCard(selectedCard)
                                }
                            }
                        }
                    } else {
                        Div({
                            classes(Styles.stickyHeader)
                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                justifyContent(JustifyContent.SpaceBetween)
                                padding(1.r)
                            }
                        }) {
                            IconButton(
                                name = "arrow_back",
                                title = appString { goBack },
                                background = true
                            ) {
                                if (cardNavHistory.isNotEmpty()) {
                                    selectedCard = cardNavHistory.last()
                                    cardNavHistory = cardNavHistory.dropLast(1)
                                    centerMapOnCard(selectedCard)
                                } else {
                                    selectedCard = null
                                }
                            }

                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    justifyContent(JustifyContent.Center)
                                    property("flex", "1")
                                    paddingLeft(1.r)
                                    paddingRight(1.r)
                                }
                            }) {
                                Span({
                                    style {
                                        fontWeight("bold")
                                    }
                                }) {
                                    Text(selectedCard?.name ?: application.appString { newCard })
                                }
                                selectedCard?.hint?.notBlank?.let { hint ->
                                    Span({
                                        style {
                                            fontSize(14.px)
                                            opacity(.75f)
                                        }
                                    }) {
                                        Text(hint)
                                    }
                                }
                            }

                            IconButton(
                                name = "open_in_new",
                                title = appString { openPage },
                                background = true
                            ) {
                                window.open("/page/${selectedCard!!.id!!}", target = "_blank")
                            }
                        }

                        Div({
                            classes(Styles.mapCardContent)
                        }) {
                            selectedCard?.let {
                                CardContent(
                                    card = it,
                                    showTitle = false,
                                    mediaStyles = {
                                        with(Styles) {
                                            mapCardMediaStyle()
                                        }
                                    },
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
                FlexInput(
                    value = searchText,
                    placeholder = appString { searchForThings },
                    singleLine = true,
                    styles = {
                        property("border", "none")
                        property("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.125)")
                    },
                    onChange = {
                        searchText = it
                    }
                )
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
                Div({
                    classes(AppStyles.tray, AppStyles.trayShadow)
                    style { alignSelf(AlignSelf.Center)
                        if (filtersExpanded) {
                            width(100.percent)
                        }
                        maxWidth(100.percent)
                        boxSizing("border-box")
                        overflow("hidden")
                        borderRadius(1.5.r)

                        if (!filtersExpanded) {
                            cursor("pointer")
                        }
                    }
                    if (!filtersExpanded) {
                        onClick {
                            filtersExpanded = true
                        }
                    }
                }) {
                    if (filtersExpanded) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Row)
                                gap(1.r)
                                alignItems(AlignItems.Start)
                            }
                        }) {
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Row)
                                    gap(1.r)
                                    alignItems(AlignItems.Center)
                                    property("flex-wrap", "wrap")
                                    property("flex", "1")
                                }
                            }) {
                                components.LabeledSwitch(
                                    value = availableNowFilter,
                                    onValue = { availableNowFilter = it },
                                    onChange = { availableNowFilter = it },
                                    title = appString { availableToday }
                                )
                                components.LabeledSwitch(
                                    value = petsFilter,
                                    onValue = { petsFilter = it },
                                    onChange = { petsFilter = it },
                                    title = appString { pets }
                                )
                                components.LabeledSwitch(
                                    value = outdoorsFilter,
                                    onValue = { outdoorsFilter = it },
                                    onChange = { outdoorsFilter = it },
                                    title = appString { outdoors }
                                )

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Row)
                                        gap(.5.r)
                                        alignItems(AlignItems.Center)
                                        flexWrap(FlexWrap.Wrap)
                                    }
                                }) {
                                    Span {
                                        appText { parking }
                                    }
                                    listOf(
                                        Parking.Bike to appString { parkingBike },
                                        Parking.Motorbike to appString { parkingMotorbike },
                                        Parking.Car to appString { parkingCar }
                                    ).forEach { (option, label) ->
                                        Button({
                                            classes(
                                                listOfNotNull(
                                                    if (parkingFilter == option) Styles.floatingButtonSelected else null,
                                                    Styles.floatingButton,
                                                    Styles.floatingButtonSmall,
                                                )
                                            )
                                            style { flexShrink(0) }
                                            onClick { parkingFilter = if (parkingFilter == option) null else option }
                                        }) {
                                            Text(label)
                                        }
                                    }
                                }

                                if (allLanguages.isNotEmpty()) {
                                    app.components.MultiSelect(
                                        selected = selectedLanguages,
                                        onSelected = { selectedLanguages = it },
                                        multiple = true
                                    ) {
                                        allLanguages.forEach { lang ->
                                            option(lang, lang)
                                        }
                                    }
                                } else if (selectedLanguages.isNotEmpty()) {
                                    LaunchedEffect(Unit) {
                                        selectedLanguages = emptyList()
                                    }
                                }
                            }

                            IconButton(
                                name = "expand_less",
                                title = appString { collapse },
                            ) {
                                filtersExpanded = false
                            }
                        }

                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Row)
                                gap(1.5.r)
                                paddingTop(.5.r)
                                alignItems(AlignItems.Center)
                                flexWrap(FlexWrap.Wrap)
                            }
                        }) {
                            components.RangeSlider(
                                minValue = ageMin ?: 0,
                                maxValue = ageMax ?: 100,
                                minLimit = 0,
                                maxLimit = 100,
                                label = appString { ageRange },
                                steps = remember {
                                    (0..25).toList() + (30..50 step 5).toList() + (60..100 step 10).toList()
                                },
                                onValueChange = { min, max ->
                                    ageMin = min.takeIf { it > 0 }
                                    ageMax = max.takeIf { it < 100 }
                                }
                            )

                            components.RangeSlider(
                                minValue = groupSizeMin ?: 1,
                                maxValue = groupSizeMax ?: 20,
                                minLimit = 1,
                                maxLimit = 20,
                                label = appString { groupSize },
                                onValueChange = { min, max ->
                                    groupSizeMin = min.takeIf { it > 1 }
                                    groupSizeMax = max.takeIf { it < 20 }
                                }
                            )
                        }
                    } else {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                justifyContent(JustifyContent.SpaceBetween)
                            }
                        }) {
                            val summary = buildString {
                                if (availableNowFilter) append(appString { availableToday } + ", ")
                                if (petsFilter) append(appString { pets } + ", ")
                                if (outdoorsFilter) append(appString { outdoors } + ", ")
                                if (parkingFilter != null) append(when (parkingFilter) {
                                    Parking.None -> appString { parkingNone }
                                    Parking.Bike -> appString { parkingBike }
                                    Parking.Motorbike -> appString { parkingMotorbike }
                                    Parking.Car -> appString { parkingCar }
                                    null -> ""
                                } + ", ")
                                if (selectedLanguages.isNotEmpty()) append(selectedLanguages.joinToString() + ", ")
                                if (ageMin != null || ageMax != null) {
                                    val min = ageMin ?: 0
                                    val max = ageMax ?: 100
                                    append("${appString { ageRange }} ${if (min == max) min else "$min-$max"}, ")
                                }
                                if (groupSizeMin != null || groupSizeMax != null) {
                                    val min = groupSizeMin ?: 1
                                    val max = groupSizeMax ?: 20
                                    append("${if (min == max) min else "$min-$max"} ${appString { people }}")
                                }
                            }.trimEnd(' ', ',')
                            Span({
                                style {
                                    overflow("hidden")
                                    whiteSpace("nowrap")
                                    property("text-overflow", "ellipsis")
                                    flex(1)
                                }
                            }) {
                                Text(summary.ifEmpty { appString { filters } })
                            }
                            IconButton(
                                name = "expand_more",
                                title = appString { expand },
                            ) {
                                filtersExpanded = true
                            }
                        }
                    }
                }
            }
        }
        if (isMobile) {
            BottomSheet(
                state = bottomSheetState,
                onStateChange = { bottomSheetState = it }
            ) {
                Div({
                    classes(Styles.navContent)
                }) {
                    if (selectedCard == null) {
                        if (shownCards.isEmpty()) {
                            Empty { appText { noCardsNearby } }
                        } else {
                            MapList(
                                cards = shownCards,
                                styles = {
                                    padding(1.r)
                                },
                                showPhoto = true
                            ) { card ->
                                cardNavHistory = emptyList()
                                selectedCard = if (selectedCard?.id == card.id) null else card
                                centerMapOnCard(selectedCard)
                            }
                        }
                    } else {
                        Div({
                            classes(Styles.stickyHeader)
                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                justifyContent(JustifyContent.SpaceBetween)
                                padding(1.r)
                            }
                        }) {
                            IconButton(
                                name = "arrow_back",
                                title = appString { goBack },
                                background = true
                            ) {
                                if (cardNavHistory.isNotEmpty()) {
                                    selectedCard = cardNavHistory.last()
                                    cardNavHistory = cardNavHistory.dropLast(1)
                                    centerMapOnCard(selectedCard)
                                } else {
                                    selectedCard = null
                                }
                            }

                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    justifyContent(JustifyContent.Center)
                                    property("flex", "1")
                                    paddingLeft(1.r)
                                    paddingRight(1.r)
                                }
                            }) {
                                Span({
                                    style {
                                        fontWeight("bold")
                                    }
                                }) {
                                    Text(selectedCard?.name ?: application.appString { newCard })
                                }
                                selectedCard?.hint?.notBlank?.let { hint ->
                                    Span({
                                        style {
                                            fontSize(14.px)
                                            opacity(.75f)
                                        }
                                    }) {
                                        Text(hint)
                                    }
                                }
                            }

                            IconButton(
                                name = "open_in_new",
                                title = appString { openPage },
                                background = true
                            ) {
                                window.open("/page/${selectedCard!!.id!!}", target = "_blank")
                            }
                        }

                        Div({
                            classes(Styles.mapCardContent)
                        }) {
                            selectedCard?.let {
                                CardContent(
                                    card = it,
                                    showTitle = false,
                                    mediaStyles = {
                                        with(Styles) {
                                            mapCardMediaStyle()
                                        }
                                    },
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
                    }
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
