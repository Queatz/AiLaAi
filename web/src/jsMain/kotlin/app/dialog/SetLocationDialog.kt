package app.dialog

import Styles
import application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.queatz.db.Geo
import lib.mapboxCss
import lib.mapboxgl
import lib.mapboxgl.MarkerOptions
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.minWidth
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

suspend fun setLocationDialog(
    initialGeo: Geo? = null,
    onRemoveLocation: (() -> Unit)? = null,
): Geo? {
    var selectedGeo by mutableStateOf(initialGeo)

    val confirmed = dialog(
        title = application.appString { atALocation },
        confirmButton = application.appString { chooseLocation },
        cancelButton = application.appString { cancel },
        enableConfirm = { selectedGeo != null },
        extraButtons = if (onRemoveLocation != null) { { resolve ->
            Button({
                classes(Styles.textButton)
                onClick {
                    onRemoveLocation()
                    resolve(false)
                }
            }) {
                Text(application.appString { remove })
            }
        } } else null
    ) { _ ->
        Div({
            style {
                width(32.r)
                height(24.r)
                minWidth(16.r)
                minHeight(12.r)
                property("position", "relative")
                property("border-radius", ".5rem")
                property("overflow", "hidden")
            }

            ref { container ->
                val __mapboxCss = mapboxCss

                mapboxgl.accessToken =
                    "pk.eyJ1IjoiamFjb2JmZXJyZXJvIiwiYSI6ImNraXdyY211eTBlMmcycW02eDNubWNpZzcifQ.1KtSoMzrPCM0A8UVtI_gdg"

                val options: mapboxgl.MapOptions = js("{}")
                val initialLngLat: mapboxgl.LngLat = js("{}")

                if (initialGeo != null) {
                    initialLngLat.lat = initialGeo.latitude
                    initialLngLat.lng = initialGeo.longitude
                    options.zoom = 13.0
                } else {
                    // Default to Saigon
                    initialLngLat.lat = 10.77564
                    initialLngLat.lng = 106.72394
                    options.zoom = 10.0
                }

                options.container = container
                options.style = "mapbox://styles/mapbox/streets-v12"
                options.center = initialLngLat
                options.hash = false

                val map = mapboxgl.Map(options)

                map.on("load") {
                    map.resize()
                }

                var currentMarker: mapboxgl.Marker? = null

                fun placeMarker(lngLat: mapboxgl.LngLat) {
                    currentMarker?.remove()
                    val markerOptions: MarkerOptions = js("{}")
                    currentMarker = mapboxgl.Marker(markerOptions)
                        .setLngLat(lngLat)
                        .addTo(map)
                    selectedGeo = Geo(
                        latitude = lngLat.lat,
                        longitude = lngLat.lng
                    )
                }

                if (initialGeo != null) {
                    val markerLngLat: mapboxgl.LngLat = js("{}")
                    markerLngLat.lat = initialGeo.latitude
                    markerLngLat.lng = initialGeo.longitude
                    placeMarker(markerLngLat)
                }

                map.on("click") { event ->
                    val mouseEvent = event.unsafeCast<mapboxgl.MapMouseEvent>()
                    placeMarker(mouseEvent.lngLat)
                }

                onDispose {
                    map.remove()
                }
            }
        })
    }

    return if (confirmed == true) selectedGeo else null
}
