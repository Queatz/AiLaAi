@file:JsModule("@vvo/tzdb")
@file:JsNonModule

package lib

external interface RawTimeZone {
    val name: String
    val rawOffsetInMinutes: Number
}

external val rawTimeZones: Array<RawTimeZone>
