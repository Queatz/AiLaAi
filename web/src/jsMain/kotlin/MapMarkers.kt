import com.queatz.db.Card
import lib.mapboxgl
import kotlin.math.abs

data class CardMarker(
    val card: Card,
    val marker: mapboxgl.Marker,
)

fun List<Double>.toLatLng(): mapboxgl.LngLat? {
    return takeIf { size == 2 }?.let { array ->
        mapboxgl.LngLat.convert(array.asReversed().toTypedArray())
    }
}

fun Card.collides(
    other: Card,
): Boolean {
    val geo = geo?.toLatLng() ?: return false
    val otherGeo = other.geo?.toLatLng() ?: return false

    return geo.distanceTo(otherGeo) < ((size ?: 0.0) + (other.size ?: 0.0)).kmToMeters
}

val Double.kmToMeters: Double get() = this * 1_000.0

operator fun Card.compareTo(
    other: Card,
): Int = (level ?: 0).compareTo(other.level ?: 0).let { compareLevel ->
    if (compareLevel != 0) {
        compareLevel
    } else {
        (size ?: 0.0).compareTo(other.size ?: 0.0)
    }
}

fun mapboxgl.Point.near(
    other: mapboxgl.Point,
    distance: Int,
): Boolean =
    abs(x - other.x) <= distance && abs(y - other.y) <= distance

fun Card.effectiveScore(
    minLevel: Int,
    maxLevel: Int,
    minSize: Double,
    maxSize: Double,
    zoomFactor: Float,
): Float {
    val normLevel = if (maxLevel == minLevel) 0.5f
    else ((level ?: 0) - minLevel).toFloat() / (maxLevel - minLevel)
    val normSize = if (maxSize == minSize) 0.5f
    else ((size ?: 0.0) - minSize).toFloat() / (maxSize - minSize).toFloat()
    // Level completely overrides size (size only matters when levels are equal)
    val score = normLevel + normSize / (maxLevel - minLevel + 2).toFloat()
    return score * (1f - 2f * zoomFactor) + zoomFactor
}
