package com.queatz.db

import kotlinx.datetime.Instant

fun List<GroupExtended>.people(): List<Person> = mapNotNull { it.members?.mapNotNull { it.person } }
    .flatten()
    .distinctBy { it.id!! }
    .sortedByDescending { it.seen ?: Instant.DISTANT_PAST }

inline fun Card.formatPay(format: PayFrequency.() -> String) = pay?.pay?.let {
    pay?.frequency?.let { frequency ->
        "$it/${frequency.format()}"
    } ?: it
}?.takeIf { it.isNotBlank() }
