package com.queatz.ailaai.extensions

import io.ktor.client.plugins.*

inline val Exception.status get() = (this as? ResponseException)?.response?.status
