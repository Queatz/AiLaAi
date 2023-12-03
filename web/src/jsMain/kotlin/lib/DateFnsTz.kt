@file:JsModule("date-fns-tz")
@file:JsNonModule

package lib

import kotlin.js.Date

external fun formatInTimeZone(date: Date, timezone: String, format: String): String

external fun getTimezoneOffset(timezone: String): Double
