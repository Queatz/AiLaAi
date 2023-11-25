package com.queatz.ailaai.extensions

sealed class SwipeResult {
    object Previous : SwipeResult()
    object Next : SwipeResult()
    class Select<E>(val item: E) : SwipeResult()
}

val <T> List<T>.ifNotEmpty get() = takeIf { isNotEmpty() }

fun List<String>.filterNotBlank() = filter { it.isNotBlank() }

fun <T> T?.inList() = this?.let(::listOf) ?: emptyList<T>()

fun <E> List<E>.next(current: E, offset: Int): E =
    get((indexOf(current) + offset).coerceIn(indices))

fun <E> List<E>.swipe(current: E, offset: Int): SwipeResult {
    val index = indexOf(current).let { if (it == -1) 0 else it } + offset

    return if (index < 0) {
        SwipeResult.Previous
    } else if (index > lastIndex) {
        SwipeResult.Next
    } else {
        SwipeResult.Select(get(index))
    }
}

fun <T> List<T>.sortedDistinct(): List<T> = groupBy { it }.let { occurrences ->
    distinct().sortedByDescending {
        occurrences[it]?.size ?: 0
    }
}
