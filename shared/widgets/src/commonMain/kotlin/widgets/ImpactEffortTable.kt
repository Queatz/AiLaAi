package com.queatz.widgets.widgets

import kotlinx.serialization.Serializable

@Serializable
data class ImpactEffortTablePoint(
    var impact: Int? = null,
    var effort: Int? = null
)

@Serializable
data class ImpactEffortTableData(
    var card: String? = null,
    var points: Map<String, ImpactEffortTablePoint>? = null
)
