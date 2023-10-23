package com.queatz.ailaai.extensions

val <T> List<T>.ifNotEmpty get() = takeIf { isNotEmpty() }

fun List<String>.filterNotBlank() = filter { it.isNotBlank() }

fun <T> T?.inList() = this?.let(::listOf) ?: emptyList<T>()

fun <E> List<E>.next(current: E, offset: Int): E =
    get((indexOf(current) + offset).coerceIn(indices))
