import com.queatz.db.Card
import com.queatz.db.PayFrequency

val Card.hint get() = listOfNotNull(
    pay?.pay?.let {
        pay?.frequency?.let { frequency ->
            "$it/${frequency.appStringShort}"
        } ?: it
    },
    location?.notBlank
).joinToString(" • ")

val PayFrequency.appStringShort get() = when (this) {
    PayFrequency.Hourly -> application.appString { inlineHour }
    PayFrequency.Daily -> application.appString { inlineDay }
    PayFrequency.Weekly -> application.appString { inlineWeekly }
    PayFrequency.Monthly -> application.appString { inlineMonthly }
    PayFrequency.Yearly -> application.appString { inlineYearly }
}

val PayFrequency.appString get() = when (this) {
    PayFrequency.Hourly -> application.appString { hourly }
    PayFrequency.Daily -> application.appString { daily }
    PayFrequency.Weekly -> application.appString { weekly }
    PayFrequency.Monthly -> application.appString { monthly }
    PayFrequency.Yearly -> application.appString { yearly }
}

fun Card.isMine(me: String?) = person == me || collaborators?.any { it == me } == true
