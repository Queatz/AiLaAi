package com.queatz.ailaai.extensions

import android.location.Location
import at.bluesource.choicesdk.maps.common.LatLng

fun LatLng.distance(to: LatLng): Float {
    return FloatArray(1).also {
        Location.distanceBetween(
            latitude,
            longitude,
            to.latitude,
            to.longitude,
            it
        )
    }[0]
}
