package com.queatz.ailaai.api

import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.Api
import com.queatz.ailaai.ErrorBlock
import com.queatz.ailaai.SuccessBlock

suspend fun Api.categories(
    geo: LatLng,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>,
) = get(
    "categories",
    mapOf(
        "geo" to "${geo.latitude},${geo.longitude}"
    ),
    onError = onError,
    onSuccess = onSuccess
)
