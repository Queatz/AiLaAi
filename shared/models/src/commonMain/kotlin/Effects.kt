package com.queatz.db

import kotlinx.serialization.Serializable

@Serializable
sealed class Effect

@Serializable
data class RainEffect(
    val amount: Double
) : Effect()
