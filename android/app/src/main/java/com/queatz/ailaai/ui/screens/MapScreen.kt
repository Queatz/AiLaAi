package com.queatz.ailaai.ui.screens

import android.Manifest
import android.graphics.Point
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.zIndex
import androidx.core.graphics.div
import androidx.core.graphics.plus
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import at.bluesource.choicesdk.maps.common.CameraPosition
import at.bluesource.choicesdk.maps.common.CameraUpdateFactory
import at.bluesource.choicesdk.maps.common.LatLng
import at.bluesource.choicesdk.maps.common.Map
import at.bluesource.choicesdk.maps.common.MapFragment
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.databinding.LayoutMapBinding
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toLatLng
import com.queatz.ailaai.extensions.toList
import com.queatz.ailaai.helpers.geoKey
import com.queatz.ailaai.ui.dialogs.EditCardDialog
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.state.latLngSaver
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.Inventory
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

class MapControl {
    internal val recenter = MutableSharedFlow<LatLng>()

    suspend fun recenter(geo: LatLng) {
        recenter.emit(geo)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    control: MapControl,
    cards: List<Card>,
    inventories: List<Inventory>,
    bottomPadding: Int,
    onCard: (String) -> Unit,
    onInventory: (String) -> Unit,
    onGeo: (LatLng) -> Unit
) {
    val context = LocalContext.current
    var position by rememberSaveable(stateSaver = latLngSaver()) {
        mutableStateOf(null)
    }
    var mapType by rememberSavableStateOf(Map.MAP_TYPE_NORMAL)
    var zoom by rememberSavableStateOf<Float?>(null)
    val scope = rememberCoroutineScope()
    val disposable = remember { CompositeDisposable() }
    var cameraPosition by rememberStateOf<CameraPosition?>(null)
    var composed by rememberStateOf(false)
    var map: Map? by remember { mutableStateOf(null) }
    var mapView: LayoutMapBinding? by remember { mutableStateOf(null) }
    val recenter = remember { MutableSharedFlow<Pair<LatLng, Float?>>() }
    var showMapClickMenu by remember { mutableStateOf<LatLng?>(null) }
    var viewport by rememberStateOf(IntSize(0, 0))
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarseLocationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    LaunchedEffect(control) {
        control.recenter.collectLatest {
            recenter.emit(it to 18f)
        }
    }

    LaunchedEffect(Unit) {
        if (position == null) {
            val geo =
                context.dataStore.data.first()[geoKey]?.split(",")?.map { it.toDouble() } // todo user LocationSelector
            position = LatLng(geo?.get(0) ?: 0.0, geo?.get(1) ?: 0.0)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            disposable.dispose()
        }
    }

    LaunchedEffect(position) {
        if (position != null) {
            onGeo(position!!)
        }
    }

    LaunchedEffect(cameraPosition) {
        mapType = if ((cameraPosition?.zoom ?: 0f) > 20f) {
            Map.MAP_TYPE_HYBRID
        } else {
            Map.MAP_TYPE_NORMAL
        }
    }

    LaunchedEffect(map, mapType) {
        map?.mapType = mapType
    }

    LaunchedEffect(Unit) {
        recenter.collect {
            map?.animateCamera(
                CameraUpdateFactory.get().newCameraPosition(
                    CameraPosition.Builder()
                        .setTarget(it.first)
                        .setZoom(it.second ?: 14f)
                        .setBearing(if (it.second == null) 0f else map?.cameraPosition?.bearing ?: 0f)
                        .setTilt(if (it.second == null) 0f else map?.cameraPosition?.tilt ?: 0f)
                        .build()
                )
            )
        }
    }

    LaunchedEffect(map) {
        recenter.emit((position ?: return@LaunchedEffect) to zoom)
    }

    val duration = 200
    var cardPositions by rememberStateOf<List<Pin>>(listOf())
    var renderedCards by rememberStateOf(listOf<Card>())

    LaunchedEffect(cards) {
        val cardsWithGeo = cards.filter { it.geo != null }
        val goneCards = renderedCards.filter { rendered -> cardsWithGeo.none { it.id == rendered.id } }
        renderedCards = cardsWithGeo + goneCards
        delay(duration.toLong())
        renderedCards = renderedCards.filter { it !in goneCards }
    }

    LaunchedEffect(map, viewport, cameraPosition, renderedCards) {
        map ?: return@LaunchedEffect
        cardPositions = renderedCards.map { card ->
            Pin(
                card,
                map!!.getProjection().toScreenLocation(card.geo!!.toLatLng()!!)
            )
        }
    }

    LaunchedEffect(map, bottomPadding) {
        map?.setPadding(0, 0, 0, bottomPadding)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        AndroidViewBinding(
            LayoutMapBinding::inflate,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onPlaced {
                    viewport = it.size
                }
        ) {
            mapView = this
            if (composed) return@AndroidViewBinding else composed = true
            mapFragmentContainerView.doOnAttach { it.doOnDetach { mapFragmentContainerView.removeAllViews() } }

            val mapFragment = mapFragmentContainerView.getFragment<MapFragment>()

            mapFragment.getMapObservable().subscribe {
                map = it
                map?.apply {
                    clear()

                    getUiSettings().apply {
                        isMapToolbarEnabled = true
                        isCompassEnabled = true
                        isMyLocationButtonEnabled = true
                    }

                    if (locationPermissionState.status == PermissionStatus.Granted || coarseLocationPermissionState.status == PermissionStatus.Granted) {
                        isMyLocationEnabled = true
                    }

                    setOnMapClickListener {
                        showMapClickMenu = it
                    }

                    setOnCameraMoveListener {
                        cameraPosition = map?.cameraPosition
                    }

                    setOnCameraIdleListener {
                        position = map?.cameraPosition?.target
                        zoom = map?.cameraPosition?.zoom
                    }

                    moveCamera(
                        CameraUpdateFactory.get().newCameraPosition(
                            CameraPosition.Builder()
                                .setTarget(position ?: return@apply)
                                .setZoom(zoom ?: 14f)
                                .build()
                        )
                    )
                }
            }.let(disposable::add)
        }
        map?.let { map ->
            val nearDistance = 32.dp.px

            fun Point.near(other: Point, distance: Int) =
                abs(x - other.x) <= distance && abs(y - other.y) <= distance

            fun List<Point>.average(): Point {
                return reduce { acc, point -> acc + point } / size.toFloat()
            }

            fun tryNav(position: Point, block: () -> Unit) {
                val nearby = cardPositions.filter { it.position != position && (it.position.near(position, nearDistance)) }
                    .map { it.position } + position
                if (nearby.size == 1) {
                    block()
                } else {
                    val geo = map.getProjection().fromScreenLocation(nearby.average())
                    scope.launch { recenter.emit(geo to map.cameraPosition.zoom + 2f) }
                }
            }

            val s = (map.cameraPosition.zoom / 18f)
                .pow(10.0f)
                .coerceIn(.75f, 2.0f)

            val inventoriesScale by animateFloatAsState(if ((zoom ?: 0f) < 16f) .5f else 1f)

            inventories.forEach { inventory ->
                key(inventory.id) {
                    var placed by remember(inventory) { mutableStateOf(false) }
                    var size by remember { mutableStateOf(IntSize(0, 0)) }
                    val pos = map.getProjection().toScreenLocation(inventory.geo!!.toLatLng()!!)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .wrapContentSize(unbounded = true)
                            .onPlaced {
                                placed = true
                                size = it.size
                            }
                            .offset((pos.x - size.width / 2).px, (pos.y - size.height).px)
                            .zIndex(1f + pos.y)
                            .graphicsLayer(
                                scaleX = s * inventoriesScale,
                                scaleY = s * inventoriesScale,
                                alpha = if (placed) 1f else 0f,
                                transformOrigin = TransformOrigin(.5f, .75f)
                            )
                    ) {
                        Image(
                            painterResource(R.drawable.chest),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    null
                                ) {
                                    onInventory(inventory.id!!)
                                }
                        )
                    }
                }
            }

            cardPositions.forEach {
                key(it.card.id) {
                    val (card, pos) = it
                    var size by remember { mutableStateOf(IntSize(0, 0)) }
                    var placed by remember(card.name) { mutableStateOf(false) }
                    val shown = cards.any { c -> c.id == card.id }
                    val scale = remember { Animatable(if (shown) 0f else 1f) }

                    val nearScale = if (cardPositions.any { it.card != card && it.position.near(pos, nearDistance / 4) }) {
                        .25f
                    } else if (cardPositions.any { it.card != card && it.position.near(pos, nearDistance) }) {
                        .5f
                    } else {
                        1f
                    }

                    val near = remember { Animatable(nearScale) }

                    LaunchedEffect(card.id, nearScale) {
                        near.animateTo(nearScale, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    }

                    LaunchedEffect(card.id, shown) {
                        if (shown) delay((100L * Random.nextFloat()).toLong())
                        scale.animateTo(if (shown) 1f else 0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .wrapContentSize(unbounded = true)
                            .onPlaced {
                                placed = true
                                size = it.size
                            }
                            .offset((pos.x - size.width / 2).px, (pos.y - size.height).px)
                            .zIndex(1f + pos.y)
                            .graphicsLayer(
                                scaleX = s * scale.value * near.value,
                                scaleY = s * scale.value * near.value,
                                alpha = if (!placed) 0f else scale.value,
                                transformOrigin = TransformOrigin(.5f, 1f)
                            )

                    ) {
                        val isNpc = !card.npc?.photo.isNullOrBlank()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(bottom = 1.pad)
                                .clickable(
                                    remember { MutableInteractionSource() },
                                    null
                                ) {
                                    tryNav(pos) {
                                        onCard(card.id!!)
                                    }
                                }
                        ) {
                            if (isNpc) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(.5f.pad),
                                    modifier = Modifier
                                        .shadow(2.elevation, MaterialTheme.shapes.medium)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(1.5f.pad)
                                        .widthIn(max = 240.dp)
                                ) {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(MaterialTheme.typography.titleMedium.toSpanStyle()) {
                                                append(card.npc?.name.orEmpty())
                                            }
                                            append(" ")
                                            withStyle(LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.secondary).toSpanStyle()) {
                                                append(card.name.orEmpty())
                                            }
                                        }
                                    )
                                    card.npc?.text?.let {
                                        Text(it)
                                    }
                                }
                            } else {
                                OutlinedText(
                                    card.name ?: "",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .widthIn(max = 120.dp)
                                )
                                card.categories?.firstOrNull()?.let { category ->
                                    OutlinedText(
                                        category,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        outlineColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        outlineWidth = 4f,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .widthIn(max = 120.dp)
                                    )
                                }
                            }
                        }

                        val photo = card.photo?.let(api::url)

                        if (isNpc) {
                            AsyncImage(
                                model = api.url(card.npc!!.photo!!),
                                contentDescription = "",
                                contentScale = ContentScale.Inside,
                                alignment = Alignment.Center,
                                modifier = Modifier
                                    .requiredWidth(128.dp)
                                    .clickable(
                                        remember { MutableInteractionSource() },
                                        null
                                    ) {
                                        tryNav(pos) {
                                            onCard(card.id!!)
                                        }
                                    }
                            )
                        } else {
                            AsyncImage(
                                model = remember(photo) {
                                    ImageRequest.Builder(context)
                                        .data(photo)
                                        .crossfade(true)
                                        .size(64)
                                        .build()
                                },
                                contentDescription = "",
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center,
                                modifier = Modifier
                                    .requiredSize(32.dp)
                                    .shadow(2.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .border(1.5f.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    .padding(1.5f.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        tryNav(pos) {
                                            onCard(card.id!!)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
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
            onCard(it.id!!)
        }
    }

    showMapClickMenu?.let { clickGeo ->
        Menu(
            {
                showMapClickMenu = null
            }
        ) {
            menuItem(stringResource(R.string.add_a_card)) {
                showMapClickMenu = null
                newCard = Card(geo = clickGeo.toList())
            }
            menuItem(stringResource(R.string.go_here)) {
                showMapClickMenu = null
                scope.launch {
                    recenter.emit(clickGeo to map?.cameraPosition?.zoom?.plus(1f)?.coerceAtLeast(14f))
                }
            }
        }
    }
}

@Composable
fun OutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.background,
    outlineColor: Color = MaterialTheme.colorScheme.onBackground,
    outlineWidth: Float = 6f,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start
) {
    Box(
        modifier = modifier
    ) {
        Text(
            text,
            color = outlineColor,
            style = style.copy(
                drawStyle = Stroke(
                    miter = outlineWidth / 2f,
                    width = outlineWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            ),
            fontWeight = fontWeight,
            textAlign = textAlign
        )
        Text(
            text,
            color = color,
            style = style,
            fontWeight = fontWeight,
            textAlign = textAlign
        )
    }
}

data class Pin(
    val card: Card,
    val position: Point
)
