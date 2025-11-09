package lib

import org.w3c.dom.HTMLElement

@JsModule("mapbox-gl")
@JsNonModule
external object mapboxgl {
    interface IControl {
        fun onAdd(map: Map): HTMLElement
        fun onRemove(map: Map)
    }
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
        var style: String
        var center: LngLat
        var zoom: Double
        var bearing: Double
        var pitch: Double
        var minZoom: Double
        var maxZoom: Double
        var minPitch: Double
        var maxPitch: Double
        var interactive: Boolean
        var bearingSnap: Double
        var pitchWithRotate: Boolean
        var clickTolerance: Double
        var attributionControl: Boolean
        var customAttribution: String
        var logoPosition: String
        var failIfMajorPerformanceCaveat: Boolean
        var preserveDrawingBuffer: Boolean
        var antialias: Boolean
        var refreshExpiredTiles: Boolean
        var maxBounds: dynamic
        var scrollZoom: dynamic
        var dragRotate: Boolean
        var dragPan: Boolean
        var keyboard: Boolean
        var doubleClickZoom: Boolean
        var touchZoomRotate: Boolean
        var trackResize: Boolean
        var renderWorldCopies: Boolean
        var projection: String
        var locale: dynamic
        var fadeDuration: Double
        var crossSourceCollisions: Boolean
    }

    interface MarkerOptions {
        var element: HTMLElement?
        var anchor: String
        var altitude: Double?
    }

    interface FlyToOptions {
        var center: LngLat
    }

    class Marker(options: MarkerOptions?) {
        fun addTo(map: Map): Marker
        fun setLngLat(lngLat: LngLat): Marker
        fun getLngLat(): LngLat
        fun getElement(): HTMLElement
        fun setAltitude(altitude: Double): Marker
        fun getAltitude(): Double
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
        fun project(latLng: LngLat, altitude: Double? = definedExternally): Point
        fun unproject(point: Point, altitude: Double? = definedExternally): LngLat
        fun getFreeCameraOptions(): dynamic
        fun on(event: String, block: (event: MapEvent) -> Unit)
        fun setStyle(style: String)
        fun resize()
        fun getTerrain(): dynamic
        fun queryTerrainElevation(lngLat: LngLat, options: dynamic = definedExternally): Double?
    }

    class GeolocateControl(options: dynamic = definedExternally) {
        fun trigger()
    }
}

fun mapboxgl.Map.getCameraLngLat(): mapboxgl.LngLat = getFreeCameraOptions().position.toLngLat() ?: getCenter()
