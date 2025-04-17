package time

import application
import kotlin.js.Date

fun formatDistanceToNow(date: Date): String {
    // Used below
    val locale = application.locale
    return lib.formatDistanceToNow(
        date = date,
        options = js("{ addSuffix: true, locale: locale }")
    )
}

fun formatDistanceToNowStrict(date: Date): String {
    // Used below
    val locale = application.locale
    return lib.formatDistanceToNowStrict(
        date = date,
        options = js("{ includeSeconds: false, locale: locale }")
    )
}

fun differenceInMinutes(date: Date, otherDate: Date): Int {
    // Used below
    val locale = application.locale
    return lib.differenceInMinutes(
        date = date,
        otherDate = otherDate,
        options = js("{ roundingMethod: \"floor\", locale: locale }")
    )
}

fun format(date: Date, format: String): String {
    // Used below
    val locale = application.locale

    return lib.format(
        date = date,
        format = format,
        options = js("{ locale: locale }")
    ).let {
        if (application.language == "vi") {
            it
                .replace("AM", "SA")
                .replace("PM", "CH");
        } else {
            it
        }
    }
}
