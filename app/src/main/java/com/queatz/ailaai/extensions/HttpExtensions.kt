package com.queatz.ailaai.extensions

import io.ktor.client.plugins.*

inline val Throwable.status get() = (this as? ResponseException)?.response?.status
