package com.queatz

val String.notBlank get() = takeIf { it.isNotBlank() }

fun <T> String.notBlank(block: (String) -> T) = notBlank?.let(block)
