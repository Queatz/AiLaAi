package com.queatz.db

import java.net.URI

actual class UrlValidator actual constructor() {
    actual fun validate(url: String): String? {
        return try {
            URI(url).toURL().toString()
        } catch (_: Throwable) {
            null
        }
    }
}
