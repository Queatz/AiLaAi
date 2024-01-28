package com.queatz.ailaai.extensions

import com.ibm.icu.text.DateFormatSymbols
import com.ibm.icu.text.RuleBasedNumberFormat

private val ordinalNumberFormat = RuleBasedNumberFormat(RuleBasedNumberFormat.ORDINAL)

val Int.dayOfWeekName get() = DateFormatSymbols.getInstance().weekdays[this]
val Int.monthName get() = DateFormatSymbols.getInstance().months[this - 1]
val Int.ordinal get() = ordinalNumberFormat.format(this)
