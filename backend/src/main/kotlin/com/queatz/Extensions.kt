package com.queatz

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import java.net.URL
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.minutes

val String.notBlank get() = takeIf { it.isNotBlank() }

fun <T> String.notBlank(block: (String) -> T) = notBlank?.let(block)

fun Instant.startOfSecond() = toJavaInstant().truncatedTo(ChronoUnit.SECONDS).toKotlinInstant()

fun Instant.startOfMinute() = toJavaInstant().truncatedTo(ChronoUnit.MINUTES).toKotlinInstant()

suspend fun delayUntilNextMinute() = delay(
    Clock.System.now().let { now -> (now + 1.minutes).startOfMinute() - now }
)

fun String.extractUrls(): List<String> {
    val urls = mutableListOf<String>()
    val words = split("\\s+".toRegex()) // Split input string by whitespace

    for (word in words) {
        try {
            val raw = word.trim(*"()[].,:;!".toCharArray())
            if ("." !in raw) continue
            val url = URL(if (raw.contains("://")) raw else "https://$raw")
            urls.add(url.toString())
        } catch (_: Throwable) {
            // Ignore URLs that couldn't be parsed
        }
    }

    return urls
}

data class OpenGraphData(
    var title: String? = null,
    var description: String? = null,
    var image: String? = null
)

fun String.extractOpenGraphData(): OpenGraphData {
    val result = OpenGraphData()

    KsoupHtmlParser(
        handler = object : KsoupHtmlHandler {
            override fun onOpenTag(name: String, attributes: Map<String, String>, isImplied: Boolean) {
                if (name == "meta") {
                    when (attributes["property"]?.lowercase()) {
                        "og:title" -> {
                            result.title = attributes["content"]
                        }
                        "og:description" -> {
                            result.description = attributes["content"]
                        }
                        "og:image" -> {
                            result.image = attributes["content"]
                        }
                    }
                }
            }
        }
    ).parseComplete(this)

    return result
}
