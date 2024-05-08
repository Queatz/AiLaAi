package com.queatz.db

import org.w3c.dom.url.URL

actual class UrlValidator actual constructor() {
    actual fun validate(url: String): String? {
        return try {
            URL(url).toString()
        } catch (_: Throwable) {
            null
        }
    }
}
