package com.queatz

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import org.apache.commons.text.StringEscapeUtils.unescapeHtml4
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.time.temporal.ChronoUnit
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.minutes

val String.notBlank get() = takeIf { it.isNotBlank() }

fun <T> String.notBlank(block: (String) -> T) = notBlank?.let(block)

fun Instant.startOfSecond() = toJavaInstant().truncatedTo(ChronoUnit.SECONDS).toKotlinInstant()

fun Instant.startOfMinute() = toJavaInstant().truncatedTo(ChronoUnit.MINUTES).toKotlinInstant()

suspend fun delayUntilNextMinute() = delay(
    Clock.System.now().let { now -> (now + 1.minutes).startOfMinute() - now }
)

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
                            result.title = attributes["content"]?.decodeHtml()
                        }
                        "og:description" -> {
                            result.description = attributes["content"]?.decodeHtml()
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

private fun String.decodeHtml() = unescapeHtml4(this).trim()

fun File.hasTransparency(): Boolean {
    return runCatching {
        val inputStream = ByteArrayInputStream(readBytes())
        val image = ImageIO.read(inputStream)

        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val pixel = image.getRGB(x, y)
                val alpha = (pixel shr 24) and 0xff
                if (alpha < 255) {
                    return true
                }
            }
        }
        return false
    }.getOrDefault(false)
}
