package lib

import kotlinx.browser.document
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.EventListener

val systemTimezone get() = js("Intl.DateTimeFormat().resolvedOptions().timeZone") as String

fun Number.toLocaleString(): String = asDynamic().toLocaleString()

val Document.hidden: Boolean get() = js("document.hidden") as Boolean

external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver) -> Unit) {
    fun observe(target: Element)
    fun unobserve(target: Element)
    fun disconnect()
}

external class ResizeObserverEntry {
    val target: Element
    val contentRect: DOMRectReadOnly
}

external class DOMRectReadOnly {
    val width: Double
    val height: Double
}

inline fun <T> jsObject(init: T.() -> Unit): T = js("{}").unsafeCast<T>().apply {
    init()
}

// todo: delete, use Kotlin versions
external object Math {
    val PI: Float
    fun random(): Float
    fun abs(value: Float): Float
    fun floor(value: Float): Float
    fun min(a: Float, b: Float): Float
}

// Fullscreen API functions
object FullscreenApi {
    val isFullscreen: Boolean
        get() = document.asDynamic().fullscreenElement != null || 
                document.asDynamic().mozFullScreenElement != null || 
                document.asDynamic().webkitFullscreenElement != null || 
                document.asDynamic().msFullscreenElement != null

    fun requestFullscreen(element: HTMLElement) {
        val requestFullscreen = element.asDynamic().requestFullscreen ?: 
                               element.asDynamic().mozRequestFullScreen ?: 
                               element.asDynamic().webkitRequestFullscreen ?: 
                               element.asDynamic().msRequestFullscreen
        requestFullscreen.call(element)
    }

    fun exitFullscreen() {
        val exitFullscreen = document.asDynamic().exitFullscreen ?: 
                            document.asDynamic().mozCancelFullScreen ?: 
                            document.asDynamic().webkitExitFullscreen ?: 
                            document.asDynamic().msExitFullscreen
        exitFullscreen.call(document)
    }

    fun addFullscreenChangeListener(listener: EventListener) {
        document.addEventListener("fullscreenchange", listener)
        document.addEventListener("mozfullscreenchange", listener)
        document.addEventListener("webkitfullscreenchange", listener)
        document.addEventListener("MSFullscreenChange", listener)
    }

    fun removeFullscreenChangeListener(listener: EventListener) {
        document.removeEventListener("fullscreenchange", listener)
        document.removeEventListener("mozfullscreenchange", listener)
        document.removeEventListener("webkitfullscreenchange", listener)
        document.removeEventListener("MSFullscreenChange", listener)
    }
}

// Canvas functions
fun HTMLCanvasElement.toDataURL(type: String = "image/png"): String {
    return this.asDynamic().toDataURL(type) as String
}
