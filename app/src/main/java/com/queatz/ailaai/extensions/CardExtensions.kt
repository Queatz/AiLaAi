package com.queatz.ailaai.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.ailaai.api.cardGroup
import app.ailaai.api.sendMessage
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.data.json
import com.queatz.db.Card
import com.queatz.db.CardAttachment
import com.queatz.db.Message
import com.queatz.db.PayFrequency
import kotlinx.serialization.encodeToString

val Card.url get() = "$appDomain/page/$id"

fun cardUrl(id: String) = "$appDomain/page/$id"
fun storyUrl(urlOrId: String) = "$appDomain/story/$urlOrId"
fun profileUrl(id: String) = "$appDomain/profile/$id"


val Card.hint @Composable get() = listOfNotNull(
    pay?.pay?.let {
        pay?.frequency?.let { frequency ->
            "$it/${frequency.appStringShort}"
        } ?: it
    },
    location?.notBlank
).joinToString(" • ")

val PayFrequency.appStringShort @Composable get() = when (this) {
    PayFrequency.Hourly -> stringResource(R.string.inlineHour)
    PayFrequency.Daily -> stringResource(R.string.inlineDay)
    PayFrequency.Weekly -> stringResource(R.string.inlineWeekly)
    PayFrequency.Monthly -> stringResource(R.string.inlineMonthly)
    PayFrequency.Yearly -> stringResource(R.string.inlineYearly)
}

val PayFrequency.appString @Composable get() = when (this) {
    PayFrequency.Hourly -> stringResource(R.string.hourly)
    PayFrequency.Daily -> stringResource(R.string.daily)
    PayFrequency.Weekly -> stringResource(R.string.weekly)
    PayFrequency.Monthly -> stringResource(R.string.monthly)
    PayFrequency.Yearly -> stringResource(R.string.yearly)
}

suspend fun Card.reply(conversation: List<String>, onSuccess: (groupId: String) -> Unit = {}) {
    api.cardGroup(id!!) { group ->
        api.sendMessage(
            group.id!!,
            Message(
                text = conversation.filterNotBlank().ifNotEmpty?.joinToString(" → "),
                attachment = json.encodeToString(CardAttachment(id!!))
            )
        ) {
            onSuccess(group.id!!)
        }
    }
}

val Card.latLng get() = geo!!.let { LatLng(it[0], it[1]) }
