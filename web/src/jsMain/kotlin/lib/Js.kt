package lib

import org.w3c.dom.Document

val systemTimezone get() = js("Intl.DateTimeFormat().resolvedOptions().timeZone") as String

fun Number.toLocaleString(): String = asDynamic().toLocaleString()

val Document.hidden: Boolean get() = js("document.hidden") as Boolean
