package com.queatz.ailaai.ui.state

import androidx.compose.runtime.saveable.Saver
import at.bluesource.choicesdk.maps.common.LatLng

fun latLngSaver() = Saver<LatLng?, List<Double>>(
    { if (it == null) null else listOf(it.latitude, it.longitude) },
    { if (it.isEmpty()) null else LatLng.getFactory().create(it[0], it[1]) })
