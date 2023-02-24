package com.queatz.ailaai.extensions

import com.queatz.ailaai.Card
import com.queatz.ailaai.appDomain

val Card.url get() = "$appDomain/card/$id"
