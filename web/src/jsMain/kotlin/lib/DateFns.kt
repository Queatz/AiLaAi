@file:JsModule("date-fns")
@file:JsNonModule

package lib

import kotlin.js.Date

external fun intlFormat(date: Date, formatOptions: dynamic = definedExternally, localeOptions: dynamic = definedExternally): String

external fun formatDistanceToNow(date: Date, options: dynamic = definedExternally): String

external fun format(date: Date, format: String, options: dynamic = definedExternally): String
external fun parse(string: String, format: String, date: Date): Date

external fun addHours(date: Date, amount: Double): Date
external fun addDays(date: Date, amount: Double): Date
external fun addWeeks(date: Date, amount: Double): Date
external fun addMonths(date: Date, amount: Double): Date
external fun addYears(date: Date, amount: Double): Date

external fun previousSunday(date: Date): Date

external fun isEqual(date: Date, dateToCompare: Date): Boolean
external fun isBefore(date: Date, dateToCompare: Date): Boolean
external fun isAfter(date: Date, dateToCompare: Date): Boolean

external fun isYesterday(date: Date): Boolean
external fun isToday(date: Date): Boolean
external fun isTomorrow(date: Date): Boolean
external fun isThisYear(date: Date): Boolean

external fun startOfDay(date: Date): Date
external fun startOfWeek(date: Date): Date
external fun startOfMonth(date: Date): Date
external fun startOfYear(date: Date): Date

external fun setMinutes(date: Date, minutes: Int): Date
external fun getMinutes(date: Date): Int
