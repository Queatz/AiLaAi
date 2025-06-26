package com.queatz.db

import kotlinx.serialization.Serializable

@Serializable
enum class Axis(val value: String) {
    X("x"),
    Y("y"),
    Z("z");

    override fun toString(): String = value

    companion object {
        fun fromString(value: String?): Axis? {
            return value?.let { entries.find { axis -> axis.value == value } }
        }
    }
}
