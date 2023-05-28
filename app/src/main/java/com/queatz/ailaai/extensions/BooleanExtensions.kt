package com.queatz.ailaai.extensions

val Boolean?.isTrue get() = this == true
val Boolean?.isFalse get() = this != true
operator fun <T> Boolean.invoke(block: () -> T): T? = if (this) block() else null
