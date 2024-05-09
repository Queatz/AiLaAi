package com.queatz.db

expect class UrlValidator() {
    /**
     * Returns a valid URL, or null if it could not be formatted
     */
    fun validate(url: String): String?
}

private val urlValidator = UrlValidator()

private val trimChars = "\"'“”()[].,:;!?".toCharArray()

private inline fun String.trimUrl(): String? =
    trim(*trimChars).takeIf { "." in it }

private inline fun String.ensureScheme() =
    if (contains("://")) this else "https://$this"

fun String.extractUrls() = mutableListOf<String>().apply {
    for (word in split("\\s+".toRegex())) {
        val raw = word.trimUrl() ?: continue
        urlValidator.validate(raw.ensureScheme())?.let {
            add(it)
        }
    }
}

fun String.matchUrls(): List<IntRange> = mutableListOf<IntRange>().apply {
    for (word in "\\S+".toRegex().findAll(this@matchUrls)) {
        val raw = word.value.trimUrl() ?: continue
        if (urlValidator.validate(raw.ensureScheme()) != null) {
            add((word.range.first + word.value.takeWhile { it in trimChars }.length)..(word.range.last - word.value.takeLastWhile { it in trimChars }.length))
        }
    }
}

fun String.split(ranges: List<IntRange>): List<Pair<String, Boolean>> = mutableListOf<Pair<String, Boolean>>().apply {
    var lastIndex = 0

    for (range in ranges) {
        if (range.first > lastIndex) {
            add(substring(lastIndex, range.first) to false)
        }
        add(substring(range.first, range.last + 1) to true)
        lastIndex = range.last + 1
    }

    if (lastIndex < length) {
        add(substring(lastIndex) to false)
    }
}

fun String.splitByUrls() = split(matchUrls())
