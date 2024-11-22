package com.queatz.ailaai.extensions

sealed class SwipeResult {
    object Previous : SwipeResult()
    object Next : SwipeResult()
    class Select<E>(val item: E) : SwipeResult()
}

val <T> List<T>.notEmpty get() = takeIf { isNotEmpty() }
val <T> List<T>.ifNotEmpty @Deprecated("Use .notEmpty") get() = notEmpty

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

fun <T> List<T>.sortedDistinct(): List<T> = groupingBy { it }.eachCount().let { occurrences ->
    distinct().sortedByDescending {
        occurrences[it] ?: 0
    }
}

fun <T> List<T>.replace(index: Int, item: T): List<T> = toMutableList().apply {
    removeAt(index)
    add(index, item)
}.toList()
