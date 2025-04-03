package lib

import org.w3c.dom.Document
import org.w3c.dom.Element

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
