import androidx.compose.runtime.Composable
import androidx.compose.web.attributes.SelectAttrsScope
import api
import app.ailaai.api.uploadPhotos
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import lib.Qr
import lib.intlFormat
import lib.parse
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.cssRem
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.*
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.js.Date
import kotlin.js.Promise
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

fun Double.toRem(): Double {
    val rootFontSizePx = document.documentElement?.let {
        window.getComputedStyle(it).fontSize.removeSuffix("px").toDoubleOrNull()
    } ?: 16.0
    return this / rootFontSizePx
}

fun Long.quantize(step: Long) = this / step * step

@Composable
fun <T> List<T>.asNaturalList(transform: (T) -> String) = when (size) {
    0 -> ""
    1 -> transform(first())
    2 -> "${transform(first())} ${appString { and }} ${transform(last())}"
    else -> {
        "${dropLast(1).joinToString(", ", transform = transform)} ${appString { and }} ${transform(last())}"
    }
}

fun <T> List<T>.asNaturalList(application: Application, transform: (T) -> String) = when (size) {
    0 -> ""
    1 -> transform(first())
    2 -> "${transform(first())} ${application.appString { inlineAnd }} ${transform(last())}"
    else -> {
        "${dropLast(1).joinToString(", ", transform = transform)} ${application.appString { inlineAnd }} ${transform(last())}"
    }
}

val formatOptions = js("""
    {
     weekday: 'long',
     year: 'numeric',
     month: 'long',
     day: 'numeric',
     hour: 'numeric',
     minute: 'numeric',
     hour12: true,
   }
""")

fun Date.format(): String {
    // Used below
    val localeCode = application.locale.code
    return intlFormat(
        date = this,
        formatOptions = formatOptions,
        localeOptions = js("{ locale: localeCode }")
    )
}

fun parseDateTime(dateStr: String, timeStr: String, date: Date = Date()) = parse(
    string = timeStr,
    format = "HH:mm",
    date = parse(dateStr, "yyyy-MM-dd", date)
)

fun SelectAttrsScope.onSelectedOptionsChange(block: (List<String>) -> Unit) {
    onChange { event ->
        block(
            event.target.selectedOptions.asList().mapNotNull { (it as? HTMLOptionElement)?.value }
        )
    }
}

/**
 * Default spacing unit = 1.cssRem
 */
val Number.r inline get() = cssRem

val String.qr: String get() {
    val bytes = Qr.createQR(this, "gif", js("{ scale: 5 }"))
    val blob = Blob(arrayOf(bytes), BlobPropertyBag("image/gif"))
    return URL.createObjectURL(blob)
}

val String.notBlank get() = takeIf { it.isNotBlank() }

val <T> List<T>.notEmpty get() = takeIf { it.isNotEmpty() }

val Element.parents get() = let { element ->
    buildList<Element> {
        var parent: Element? = element
        while (parent != null) {
            add(parent)
            parent = parent.parentElement
        }
    }
}

fun IntRange.token() = joinToString("") { Random.nextInt(35).toString(36) }

fun AttrsScope<HTMLElement>.focusable() {
    tabIndex(1)

    onKeyDown {
        if (it.key == "Enter") {
            it.preventDefault()
            (it.target as HTMLElement).click()
        }
    }
}


fun pickPhotos(multiple: Boolean = true, block: (List<File>) -> Unit) {
    val element = document.createElement("input") as HTMLInputElement
    element.type = "file"
    element.multiple = multiple
    element.accept = "image/*"
    element.addEventListener("change", {
        if (element.files != null) {
            block(element.files!!.asList())
        }
    })
    element.click()
}

fun pickAudio(block: (File) -> Unit) {
    val element = document.createElement("input") as HTMLInputElement
    element.type = "file"
    element.multiple = false
    element.accept = "audio/*"
    element.addEventListener("change", {
        if (element.files != null && element.files!!.length > 0) {
            block(element.files!!.item(0)!!)
        }
    })
    element.click()
}

// Returns null if scaling is not needed
suspend fun HTMLImageElement.scaleToBlob(maxSize: Int): Blob? {
    val canvas = document.createElement("canvas") as HTMLCanvasElement

    if (width < maxSize && height < maxSize) {
        return null
    }

    val ratio = min(maxSize.toDouble() / width.toDouble(), maxSize.toDouble() / height.toDouble())
    val width = round(width * ratio)
    val height = round(height * ratio)

    canvas.width = width.toInt()
    canvas.height = height.toInt()

    (canvas.getContext("2d") as CanvasRenderingContext2D).apply {
        drawImage(this@scaleToBlob, .0, .0, width, height)
    }

    val  result = CompletableDeferred<Blob>()

    canvas.toBlob({
        if (it == null) {
            result.completeExceptionally(Throwable("Failed to convert canvas to blob"))
        } else {
            result.complete(it)
        }
    }, "image/jpeg", 92)

    return result.await()
}

suspend fun File.toScaledBlob(maxSize: Int): Blob {
    val result = CompletableDeferred<HTMLImageElement>()
    val reader = FileReader()
    reader.onload = {
        val img = document.createElement("img") as HTMLImageElement
        img.onerror = { _, _, _, _, _ ->
            result.completeExceptionally(Throwable("Error reading image"))
            Unit
        }
        img.onload = {
            result.complete(img)
            Unit
        }

        img.src = reader.result as String // data url

        Unit
    }

    reader.onerror = {
        result.completeExceptionally(Throwable("Error reading file"))
    }

    reader.readAsDataURL(this)

    return result.await().scaleToBlob(maxSize) ?: this
}

fun web.blob.Blob.downloadAsAudio() {
    val url = web.url.URL.createObjectURL(this)
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = url
    a.download = "audio.webm"
    document.body!!.appendChild(a)
    a.click()
    document.body!!.removeChild(a)
    web.url.URL.revokeObjectURL(url)

}

suspend fun File.toScaledBytes(maxSize: Int = 1600) =
    toScaledBlob(maxSize).toBytes()

/**
 * Scales and crops an image to create a game tile.
 * The longest edge will be scaled to 64px and the image will be center-cropped to make it square.
 */
suspend fun File.toGameTileBlob(): Blob {
    val result = CompletableDeferred<HTMLImageElement>()
    val reader = FileReader()
    reader.onload = {
        val img = document.createElement("img") as HTMLImageElement
        img.onerror = { _, _, _, _, _ ->
            result.completeExceptionally(Throwable("Error reading image"))
            Unit
        }
        img.onload = {
            result.complete(img)
            Unit
        }

        img.src = reader.result as String // data url
        Unit
    }

    reader.onerror = {
        result.completeExceptionally(Throwable("Error reading file"))
    }

    reader.readAsDataURL(this)

    val img = result.await()

    // Create a canvas for the scaled and cropped image
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

    // Set canvas size to 64x64 (final tile size)
    canvas.width = 64
    canvas.height = 64

    // Calculate scaling factor to make longest edge 64px
    val scaleFactor = 64.0 / maxOf(img.width, img.height)
    val scaledWidth = img.width * scaleFactor
    val scaledHeight = img.height * scaleFactor

    // Calculate position to center the image
    val offsetX = (64 - scaledWidth) / 2
    val offsetY = (64 - scaledHeight) / 2

    // Draw the scaled and centered image
    ctx.drawImage(img, offsetX, offsetY, scaledWidth, scaledHeight)

    // Convert canvas to blob
    val blobResult = CompletableDeferred<Blob>()

    canvas.toBlob({
        if (it == null) {
            blobResult.completeExceptionally(Throwable("Failed to convert canvas to blob"))
        } else {
            blobResult.complete(it)
        }
    }, "image/png", 92)

    return blobResult.await()
}

/**
 * Picks a photo, processes it for use as a game tile (scales and center-crops),
 * uploads it, and returns the URL of the uploaded photo.
 */
suspend fun pickGameTilePhoto(onSuccess: (String) -> Unit) {
    val deferred = CompletableDeferred<Unit>()

    pickPhotos(multiple = false) { files ->
        if (files.isNotEmpty()) {
            val file = files.first()

            // Use kotlinx.coroutines.GlobalScope to launch a coroutine
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    // Process the image (scale and center-crop)
                    val processedBlob = file.toGameTileBlob()
                    val processedBytes = processedBlob.toBytes()

                    // Upload the processed image
                    api.uploadPhotos(
                        photos = listOf(processedBytes),
                        onSuccess = { response ->
                            if (response.urls.isNotEmpty()) {
                                onSuccess(response.urls.first())
                            }
                            deferred.complete(Unit)
                        },
                        onError = { error -> 
                            console.error("Error uploading game tile photo", error)
                            deferred.complete(Unit)
                        }
                    )
                } catch (e: Throwable) {
                    console.error("Error processing game tile photo", e)
                    deferred.complete(Unit)
                }
            }
        } else {
            deferred.complete(Unit)
        }
    }

    // Wait for the upload to complete
    deferred.await()
}


suspend fun Blob.toBytes(): ByteArray {
    val reader = asDynamic().stream().getReader()
    var bytes = ByteArray(0)
    while (true) {
        val chunk = (reader.read() as Promise<*>).await().asDynamic()
        val value = chunk.value as? Uint8Array
        if (value != null) {
            bytes += ByteArray(value.length) { value[it] }
        }
        if (chunk.done == true) {
            break
        }
    }
    return bytes
}

fun <T> List<T>.sortedDistinct(): List<T> = groupingBy { it }.eachCount().let { occurrences ->
    distinct().sortedByDescending {
        occurrences[it] ?: 0
    }
}

fun bulletedString(vararg items: String?) = items.filterNotNull().joinToString(" • ")

fun String.ellipsize(maxLength: Int = 128) = if (length <= maxLength) this else this.take(maxLength - 1) + "…"

/**
 * Formats a number to 1 decimal place
 */
fun Number.format1Decimal(): String {
    return (round(this.toDouble() * 10) / 10).toString().let {
        if (it.endsWith(".0")) it.removeSuffix(".0") else it
    }
}

fun Int.withPlus() = let {
    if (it > 0) {
        "+$it"
    } else {
        it.toString()
    }
}

/**
 * Helper function to convert a Blob to an HTMLImageElement
 */
suspend fun Blob.toImage(): HTMLImageElement {
    val result = CompletableDeferred<HTMLImageElement>()
    val url = URL.createObjectURL(this)

    val img = document.createElement("img") as HTMLImageElement
    img.onload = {
        URL.revokeObjectURL(url)
        result.complete(img)
        Unit
    }
    img.onerror = { _, _, _, _, _ ->
        URL.revokeObjectURL(url)
        result.completeExceptionally(Throwable("Failed to load image"))
        Unit
    }
    img.src = url

    return result.await()
}

/**
 * Crops an image to remove transparent areas around the edges.
 * @param blob The image blob or File to crop
 * @param alphaThreshold The threshold below which a pixel is considered transparent (0-255)
 * @return A blob containing the cropped image, or null if cropping failed
 */
suspend fun cropTransparentEdges(blob: Blob, alphaThreshold: Int = 1): Blob? {
    // Convert blob to an HTMLImageElement
    val img = blob.toImage()

    // Create a canvas to analyze the image
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

    // Set canvas size to match image
    canvas.width = img.width
    canvas.height = img.height

    // Draw the image on the canvas
    ctx.drawImage(img, 0.0, 0.0)

    // Get the image data to analyze pixels
    val imageData = ctx.getImageData(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
    val data = imageData.data

    // Find the bounds of non-transparent content
    var minX = canvas.width
    var minY = canvas.height
    var maxX = 0
    var maxY = 0

    // Scan all pixels to find the bounding box of non-transparent content
    for (y in 0 until canvas.height) {
        for (x in 0 until canvas.width) {
            val index = (y * canvas.width + x) * 4
            val alpha = data[index + 3]

            if (alpha >= alphaThreshold) {
                minX = minOf(minX, x)
                minY = minOf(minY, y)
                maxX = maxOf(maxX, x)
                maxY = maxOf(maxY, y)
            }
        }
    }

    // If no non-transparent pixels were found, return the original image
    if (minX > maxX || minY > maxY) {
        return blob
    }

    // Add a small padding (optional)
    val padding = 0
    minX = maxOf(0, minX - padding)
    minY = maxOf(0, minY - padding)
    maxX = minOf(canvas.width - 1, maxX + padding)
    maxY = minOf(canvas.height - 1, maxY + padding)

    // Calculate dimensions of the cropped image
    val width = maxX - minX + 1
    val height = maxY - minY + 1

    // Create a new canvas for the cropped image
    val croppedCanvas = document.createElement("canvas") as HTMLCanvasElement
    val croppedCtx = croppedCanvas.getContext("2d") as CanvasRenderingContext2D

    croppedCanvas.width = width
    croppedCanvas.height = height

    // Draw the cropped portion of the image
    croppedCtx.drawImage(
        img,
        minX.toDouble(), minY.toDouble(), width.toDouble(), height.toDouble(),
        0.0, 0.0, width.toDouble(), height.toDouble()
    )

    // Convert the canvas to a blob
    val blobResult = CompletableDeferred<Blob>()

    croppedCanvas.toBlob({
        if (it == null) {
            blobResult.completeExceptionally(Throwable("Failed to convert canvas to blob"))
        } else {
            blobResult.complete(it)
        }
    }, "image/png")

    return blobResult.await()
}
