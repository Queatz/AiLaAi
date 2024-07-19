import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.AppPage
import app.ailaai.api.cards
import com.queatz.db.Card
import com.queatz.db.Geo
import components.AppHeader
import components.SearchField
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import lib.getCameraLngLat
import lib.mapboxgl
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundRepeat
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun MainPage() {
    if (true || application.me.value == null) {
        var searchText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var searchResults by remember { mutableStateOf(listOf<Card>()) }
        var geo by remember { mutableStateOf<Geo?>(null) }
        var map by remember { mutableStateOf<mapboxgl.Map?>(null) }
        var markers by remember { mutableStateOf(emptyList<mapboxgl.Marker>()) }

        LaunchedEffect(geo, searchText) {
            geo ?: return@LaunchedEffect
            isLoading = true
            delay(250)

            api.cards(geo!!, search = searchText.notBlank, public = true) {
                searchResults = it
            }

            isLoading = false
        }

        fun update() {
            map ?: return

            val cameraLngLat = map!!.getCameraLngLat()
            val altitude = map!!.getFreeCameraOptions().position.toAltitude() as Double
            console.log(altitude)

            markers.forEach {
                val groundDistance = cameraLngLat.distanceTo(it.getLngLat())
                val scale = 100.0 / sqrt(groundDistance.pow(2.0) + altitude.pow(2.0))
                val element = it.getElement().firstElementChild as HTMLElement
                element.style.transform = "scale(${scale.coerceAtLeast(0.125)})"
                it.getElement().style.zIndex = (scale * 1000.0).toInt().toString()
            }
        }

        LaunchedEffect(searchResults) {
            markers.forEach {
                it.remove()
                console.log("removed", it.getElement())
            }

            markers = searchResults.map { card ->
                val element = document.createElement("div") as HTMLDivElement

                val options: mapboxgl.MarkerOptions = js("{}")
                options.element = element
                options.anchor = "bottom"

                val latlng: mapboxgl.LngLat = js("{}")
                latlng.lat = card.geo!![0]
                latlng.lng = card.geo!![1]

                mapboxgl.Marker(options)
                    .setLngLat(latlng)
                    .addTo(map!!).also {
                        renderComposable(root = element) {
//                            Text("${card.name}")
                            Div({
                                onClick {
                                    window.open("$webBaseUrl/page/${card.id!!}", target = "_blank")
                                }

                                style {
                                    cursor("pointer")
                                    width(16.r)
                                    height(16.r)
                                    property("will-change", "transform")
                                    property("transform-origin", "bottom center")
                                    backgroundRepeat("no-repeat")

                                    if (card.photo.isNullOrBlank()) {
                                        backgroundColor(Styles.colors.background)
                                        borderRadius(16.r)
                                    } else {
                                        if (card.name?.contains("HYPER") == true) {
                                            backgroundImage("url(https://api.ailaai.app/static/photo/group-32807229-40915952-photo.jpg)")
                                            backgroundPosition("center")
                                            backgroundSize("contain")
                                        } else {
                                            borderRadius(16.r)
                                            backgroundImage("url($baseUrl${card.photo!!})")
                                            backgroundPosition("center")
                                            backgroundSize("cover")
                                            property("border", "${2.px} solid ${Color.white}")
                                        }
                                    }
                                }
                            }) {}
                        }
                    }
            }

            update()
        }

        Div({
            classes(Styles.mainContainer)
        }) {
           Div({
                style {
                    property("inset", "0")
                    position(Position.Absolute)
                }

                ref { ref ->
                    val options: mapboxgl.MapOptions = js("{}")
                    options.container = ref
                    options.boxZoom = false
                    options.hash = true
                    map = mapboxgl.Map(options).apply {
                        addControl(mapboxgl.GeolocateControl(), "bottom-right")

                        on("load") {
                            geo = getCameraLngLat().let { Geo(it.lat, it.lng) }
                        }

                        on("moveend") {
                            geo = getCameraLngLat().let { Geo(it.lat, it.lng) }
                        }

                        on("render") {
                            update()
                        }
                    }

                    onDispose {
                        map?.remove()
                    }
                }
            }) {}

            AppHeader(appString { appName }, background = false)

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    maxWidth(800.px)
                    width(100.percent)
                    alignSelf(AlignSelf.Center)
                }
            }) {
                SearchField(searchText, appString { searchCity },
                    shadow = true,
                    styles = {
                }) {
                    searchText = it
                }
            }
        }
    } else {
        AppPage()
    }
}
