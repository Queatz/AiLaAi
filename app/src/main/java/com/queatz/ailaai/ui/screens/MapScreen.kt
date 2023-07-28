package com.queatz.ailaai.ui.screens

import android.graphics.Point
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.zIndex
import androidx.core.graphics.div
import androidx.core.graphics.plus
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.navigation.NavController
import at.bluesource.choicesdk.maps.common.*
import at.bluesource.choicesdk.maps.common.Map
import coil.compose.AsyncImage
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.cards
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.databinding.LayoutMapBinding
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toLatLng
import com.queatz.ailaai.helpers.geoKey
import com.queatz.ailaai.ui.state.latLngSaver
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

@OptIn(ExperimentalTextApi::class)
@Composable
fun MapScreen(navController: NavController, me: () -> Person?) {
    var position by rememberSaveable(stateSaver = latLngSaver()) {
        mutableStateOf(null)
    }
    var cards by rememberStateOf(emptyList<Card>())
    var mapType by rememberSavableStateOf(Map.MAP_TYPE_NORMAL)
    val scope = rememberCoroutineScope()
    val disposable = remember { CompositeDisposable() }
    var cameraPosition by rememberStateOf<CameraPosition?>(null)

    val context = LocalContext.current

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

    var composed by rememberStateOf(false)
    var map: Map? by remember { mutableStateOf(null) }
    var mapView: LayoutMapBinding? by remember { mutableStateOf(null) }
    val recenter = remember { MutableSharedFlow<Pair<LatLng, Float?>>() }

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

    suspend fun reload() {
        position = map?.cameraPosition?.target
        api.cards(position ?: return, limit = 10) {
            cards = it // todo exclude equipped cards at api layer
        }
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
        recenter.emit((position ?: return@LaunchedEffect) to null)
    }

    val duration = 200
    var cardPositions by rememberStateOf<List<Pin>>(listOf())
    var renderedCards by rememberStateOf(listOf<Card>())

    LaunchedEffect(cards) {
        val goneCards = renderedCards.filter { rendered -> cards.none { it.id == rendered.id } }
        renderedCards = cards + goneCards
        delay(duration.toLong())
        renderedCards -= goneCards
    }

    LaunchedEffect(map, cameraPosition, renderedCards) {
        map ?: return@LaunchedEffect

        cardPositions = renderedCards.map { card ->
            Pin(
                card,
                map!!.getProjection().toScreenLocation(card.geo!!.toLatLng()!!)
            )
        }
    }

    Box {
        AndroidViewBinding(
            LayoutMapBinding::inflate,
            modifier = Modifier
                .fillMaxSize()
        ) {
            mapView = this
            if (composed) return@AndroidViewBinding else composed = true
            mapFragmentContainerView.doOnAttach { it.doOnDetach { mapFragmentContainerView.removeAllViews() } }

            val mapFragment = mapFragmentContainerView.getFragment<MapFragment>()

            mapFragment.getMapObservable().subscribe {
                map = it
                map?.apply {
                    clear()

                    getUiSettings().isMapToolbarEnabled = true
                    getUiSettings().isMyLocationButtonEnabled = true

                    setOnMapClickListener {
                        position = it
                    }

                    setOnCameraMoveListener {
                        cameraPosition = map?.cameraPosition
                    }

                    setOnCameraIdleListener {
                        scope.launch {
                            reload()
                        }
                    }

                    moveCamera(
                        CameraUpdateFactory.get().newCameraPosition(
                            CameraPosition.Builder()
                                .setTarget(position ?: return@apply)
                                .setZoom(14f)
                                .build()
                        )
                    )
                }
            }.let(disposable::add)
        }
        map?.let { map ->
            fun Point.near(other: Point, distance: Int) =
                abs(x - other.x) <= distance && abs(y - other.y) <= distance

            fun List<Point>.average(): Point {
                return reduce { acc, point -> acc + point } / size.toFloat()
            }

            fun tryNav(position: Point, block: () -> Unit) {
                val nearby = cardPositions.filter { it.position != position && (it.position.near(position, 100)) }
                    .map { it.position } + position
                if (nearby.size == 1) {
                    block()
                } else {
                    val geo = map.getProjection().fromScreenLocation(nearby.average())
                    scope.launch { recenter.emit(geo to map.cameraPosition.zoom + 2f) }
                }
            }

            cardPositions.sortedBy { it.card.id ?: "" }.forEach {
                val (card, pos) = it
                var size by rememberStateOf(IntSize(0, 0))
                val s = (map.cameraPosition.zoom / 16f).toDouble().pow(10.0).coerceAtLeast(.75).coerceAtMost(2.0).toFloat()
                val shown = it.card in cards
                val scale = remember { Animatable(if (shown) 0f else 1f) }
                var placed by rememberStateOf(false)
                var name by rememberStateOf("")

                if (name != it.card.name) {
                    placed = false
                    name = it.card.name ?: ""
                }

                LaunchedEffect(shown) {
                    if (shown) delay(25L * cardPositions.indexOf(it))
                    scale.animateTo(if (shown) 1f else 0f, tween(duration))
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
                            scaleX = s * scale.value,
                            scaleY = s * scale.value,
                            alpha = if (!placed) 0f else scale.value,
                            transformOrigin = TransformOrigin(.5f, 1f)
                        )

                ) {
                    OutlinedText(
                        card.name ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable(
                                MutableInteractionSource(),
                                null
                            ) {
                                tryNav(pos) {
                                    navController.navigate("card/${card.id!!}")
                                }
                            }
                            .widthIn(max = 120.dp)
                    )
                    card.categories?.firstOrNull()?. let { category ->
                        OutlinedText(
                            category,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            outlineColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            outlineWidth = 4f,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(bottom = PaddingDefault)
                                .clickable(
                                    MutableInteractionSource(),
                                    null
                                ) {
                                    tryNav(pos) {
                                        navController.navigate("card/${card.id!!}")
                                    }
                                }
                                .widthIn(max = 120.dp)
                        )
                    }
                    AsyncImage(
                        model = card.photo?.let(api::url),
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
                                    navController.navigate("card/${card.id!!}")
                                }
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
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
