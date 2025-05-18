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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

val String.notBlank get() = takeIf { it.isNotBlank() }

fun <T> String.notBlank(block: (String) -> T) = notBlank?.let(block)

fun Instant.startOfSecond() = toJavaInstant().truncatedTo(ChronoUnit.SECONDS).toKotlinInstant()

fun Instant.startOfMinute() = toJavaInstant().truncatedTo(ChronoUnit.MINUTES).toKotlinInstant()

fun Instant.startOfHour() = toJavaInstant().truncatedTo(ChronoUnit.HOURS).toKotlinInstant()

suspend fun delayUntilNextMinute() = delay(
    Clock.System.now().let { now -> (now + 1.minutes).startOfMinute() - now }
)

suspend fun delayUntilNextHour() = delay(
    Clock.System.now().let { now -> (now + 1.hours).startOfHour() - now }
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
                if (alpha == 127) {
                    return true
                }
            }
        }
        return false
    }.getOrDefault(false)
}

/**
 * Crops an image by removing transparent areas around the edges.
 * @param threshold The alpha threshold below which a pixel is considered transparent (0.0-1.0)
 * @return A pair containing the cropped image and its dimensions (width, height)
 */
fun ByteArray.cropTransparentBackground(threshold: Double = 0.2): Pair<ByteArray, Pair<Int, Int>> {
    return runCatching {
        val inputStream = ByteArrayInputStream(this)
        val image = ImageIO.read(inputStream)

        // Find the bounds of the non-transparent area
        var minX = image.width
        var minY = image.height
        var maxX = 0
        var maxY = 0

        val alphaThreshold = (threshold * 255).toInt()

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.getRGB(x, y)
                val alpha = (pixel shr 24) and 0xff

                if (alpha > alphaThreshold) {
                    minX = minX.coerceAtMost(x)
                    minY = minY.coerceAtMost(y)
                    maxX = maxX.coerceAtLeast(x)
                    maxY = maxY.coerceAtLeast(y)
                }
            }
        }

        // If the image is completely transparent or the bounds are invalid
        if (minX >= maxX || minY >= maxY) {
            return@runCatching Pair(this, Pair(image.width, image.height))
        }

        // Create a new image with the cropped dimensions
        val croppedWidth = maxX - minX + 1
        val croppedHeight = maxY - minY + 1
        val croppedImage = BufferedImage(croppedWidth, croppedHeight, BufferedImage.TYPE_INT_ARGB)

        // Copy the non-transparent area to the new image
        val g = croppedImage.createGraphics()
        g.drawImage(image.getSubimage(minX, minY, croppedWidth, croppedHeight), 0, 0, null)
        g.dispose()

        // Convert the cropped image back to bytes
        val outputStream = java.io.ByteArrayOutputStream()
        ImageIO.write(croppedImage, "png", outputStream)

        Pair(outputStream.toByteArray(), Pair(croppedWidth, croppedHeight))
    }.getOrDefault(Pair(this, Pair(0, 0)))
}
