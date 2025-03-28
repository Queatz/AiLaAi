package lib

import org.w3c.dom.HTMLElement

@JsModule("mapbox-gl")
@JsNonModule
external object mapboxgl {
    var accessToken: String

    interface MapMouseEvent {
        var lngLat: LngLat
    }

    interface MapEvent {
    }
    
    interface LngLat {
        var lng: Double
        var lat: Double

        fun distanceTo(other: LngLat): Double

        companion object {
            fun convert(array: Array<Double>): mapboxgl.LngLat
        }
    }

    interface Point {
        var x: Double
        var y: Double
    }

    interface MapOptions {
        var container: HTMLElement
        var boxZoom: Boolean
        var hash: Boolean
    }

    interface MarkerOptions {
        var element: HTMLElement?
        var anchor: String
    }

    interface FlyToOptions {
        var center: LngLat
    }

    class Marker(options: dynamic) {
        fun addTo(map: Map): Marker
        fun setLngLat(lngLat: LngLat): Marker
        fun getLngLat(): LngLat
        fun getElement(): HTMLElement
        fun remove()
    }

    class Map(options: MapOptions) {
        fun addControl(control: dynamic, position: String)
        fun remove()
        fun getCenter(): LngLat
        fun setCenter(lngLat: LngLat)
        fun flyTo(options: FlyToOptions)
        fun getZoom(): Double
        fun getPitch(): Double
        fun project(latLng: LngLat): Point
        fun getFreeCameraOptions(): dynamic
        fun on(event: String, block: (event: mapboxgl.MapEvent) -> Unit)
    }

    class GeolocateControl(options: dynamic = definedExternally) {
        fun trigger()
    }
}

fun mapboxgl.Map.getCameraLngLat(): mapboxgl.LngLat = getFreeCameraOptions().position.toLngLat() ?: getCenter()
