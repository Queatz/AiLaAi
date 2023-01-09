package com.queatz.ailaai.extensions

inline fun <T> Set<T>.toggle(value: T) = if (contains(value)) this - value else this + value
