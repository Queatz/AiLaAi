package com.queatz.ailaai.extensions

import com.queatz.ailaai.*

val Card.url get() = "$appDomain/card/$id"

fun GroupExtended.name(someone: String) =
    group?.name?.nullIfBlank
        ?: members?.mapNotNull { it.person }?.joinToString { it.name ?: someone }
        ?: ""

fun GroupExtended.photos(omit: List<Person> = emptyList()) = members
    ?.filter {
        omit.none { person -> it.person?.id == person.id }
    }
    ?.map {
        it.person?.photo ?: ""
    } ?: listOf("")
