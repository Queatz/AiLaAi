import androidx.compose.runtime.Composable
import androidx.compose.web.attributes.SelectAttrsScope
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
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

@Composable
fun <T> List<T>.asNaturalList(transform: (T) -> String) = when (size) {
    0 -> ""
    1 -> transform(first())
    2 -> "${transform(first())} ${appString { and }} ${transform(last())}"
    else -> {
        "${dropLast(1).joinToString(", ", transform = transform)} ${appString { and }} ${transform(last())}"
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

fun Date.format() = intlFormat(this, formatOptions, js("{ locale: \"en-US\" }"))

fun parseDateTime(dateStr: String, timeStr: String, date: Date = Date()) = parse(
    timeStr,
    "HH:mm",
    parse(dateStr, "yyyy-MM-dd", date)
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


suspend fun File.toScaledBytes(maxSize: Int = 1600) =
    toScaledBlob(maxSize).toBytes()


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
