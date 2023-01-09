package com.queatz.ailaai.extensions

inline fun <T> Set<T>.toggle(value: T) = if (contains(value)) this - value else this + value
inline fun <T> List<T>.toggle(value: T, predicate: (T) -> Boolean) = find(predicate)?.let { this - it } ?: (this + value)
