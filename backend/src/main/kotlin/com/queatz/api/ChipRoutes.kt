package com.queatz.api

import com.queatz.chipService
import com.queatz.db.ChipFeed
import com.queatz.db.ChipFormatType
import com.queatz.db.ChipPreviewRequest
import com.queatz.db.ImportChipsBody
import com.queatz.db.PersonChip
import com.queatz.db.PersonChipExtended
import com.queatz.db.ReorderChipsBody
import com.queatz.db.asKey
import com.queatz.db.defaultChipHexColor
import com.queatz.db.parseChipHexColor
import com.queatz.db.personChipExtended
import com.queatz.db.personChips
import com.queatz.db.personChipsExtended
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

fun Route.chipRoutes() {
    authenticate {
        get("/chips") {
            val personKey = me.id!!.asKey()
            respond {
                val myChips = db.personChips(personKey)
                val distinctUrls = myChips
                    .mapNotNull { chipService.normalizeUrl(it.url) }
                    .distinct()

                if (distinctUrls.isNotEmpty()) {
                    coroutineScope {
                        distinctUrls.map { url ->
                            async {
                                chipService.ensureAndSyncSubscribedFeed(url, force = false)
                            }
                        }.awaitAll()
                    }
                }

                db.personChipsExtended(personKey)
            }
        }

        post("/chips") {
            val personKey = me.id!!.asKey()
            val body = call.receive<PersonChip>()
            respond {
                val existing = db.personChips(personKey)
                val isAbove = body.isAbove ?: false
                val nextOrder = existing
                    .filter { (it.isAbove != false) == isAbove }
                    .maxOfOrNull { (it.order ?: 0) + 1 }
                    ?: existing.size

                val normalizedUrl = chipService.normalizeUrl(body.url)
                val feed = normalizedUrl?.let { chipService.ensureAndSyncSubscribedFeed(it, force = false) }

                val inserted = db.insert(
                    PersonChip(
                        person = personKey,
                        feed = feed?.id?.asKey(),
                        name = body.name?.trim().orEmpty(),
                        url = normalizedUrl ?: body.url?.trim().orEmpty(),
                        format = body.format ?: ChipFormatType.TEXT,
                        prefix = body.prefix.orEmpty(),
                        color = parseChipHexColor(body.color)?.toHex() ?: defaultChipHexColor,
                        jsonPath = body.jsonPath?.trim().orEmpty(),
                        isAbove = isAbove,
                        order = body.order ?: nextOrder,
                    )
                )

                chipService.notifyPersonWebTabs(me.id!!)

                PersonChipExtended(
                    chip = inserted.apply { this.feed = null },
                    value = feed?.value,
                    syncedAt = feed?.syncedAt,
                )
            }
        }

        post("/chips/import") {
            val personKey = me.id!!.asKey()
            val body = call.receive<ImportChipsBody>()
            respond {
                val existing = db.personChips(personKey)
                if (existing.isEmpty() && body.chips.isNotEmpty()) {
                    val distinctUrls = body.chips
                        .mapNotNull { chipService.normalizeUrl(it.url) }
                        .distinct()
                    val feedsByUrl = coroutineScope {
                        distinctUrls.map { url ->
                            async {
                                url to chipService.ensureAndSyncSubscribedFeed(url, force = false)
                            }
                        }.awaitAll().toMap()
                    }

                    body.chips.take(50).forEachIndexed { index, incoming ->
                        val normalizedUrl = chipService.normalizeUrl(incoming.url)
                        val feed = normalizedUrl?.let { feedsByUrl[it] }
                        db.insert(
                            PersonChip(
                                person = personKey,
                                feed = feed?.id?.asKey(),
                                name = incoming.name?.trim().orEmpty(),
                                url = normalizedUrl ?: incoming.url?.trim().orEmpty(),
                                format = incoming.format ?: ChipFormatType.TEXT,
                                prefix = incoming.prefix.orEmpty(),
                                color = parseChipHexColor(incoming.color)?.toHex() ?: defaultChipHexColor,
                                jsonPath = incoming.jsonPath?.trim().orEmpty(),
                                isAbove = incoming.isAbove ?: true,
                                order = incoming.order ?: index,
                            )
                        )
                    }
                    chipService.notifyPersonWebTabs(me.id!!)
                }

                db.personChipsExtended(personKey)
            }
        }

        post("/chips/reorder") {
            val personKey = me.id!!.asKey()
            val body = call.receive<ReorderChipsBody>()
            respond {
                val existingByKey = db.personChips(personKey).associateBy { it.id?.asKey() }
                body.items.forEach { item ->
                    val chip = existingByKey[item.id.asKey()] ?: return@forEach
                    if (chip.order != item.order || chip.isAbove != item.isAbove) {
                        chip.order = item.order
                        chip.isAbove = item.isAbove
                        db.update(chip)
                    }
                }
                chipService.notifyPersonWebTabs(me.id!!)
                db.personChipsExtended(personKey)
            }
        }

        post("/chips/preview") {
            val body = call.receive<ChipPreviewRequest>()
            respond {
                chipService.previewUrl(body.url)
            }
        }

        post("/chips/{id}") {
            val personKey = me.id!!.asKey()
            val chipKey = parameter("id").asKey()
            val body = call.receive<PersonChip>()
            respond {
                val existing = db.document(PersonChip::class, chipKey)
                    ?: return@respond HttpStatusCode.NotFound
                if (existing.person != personKey) {
                    return@respond HttpStatusCode.Forbidden
                }

                val previousFeedKey = existing.feed
                val normalizedUrl = chipService.normalizeUrl(body.url)
                val newFeed = normalizedUrl?.let { chipService.ensureAndSyncSubscribedFeed(it, force = false) }
                val newFeedKey = newFeed?.id?.asKey()

                existing.feed = newFeedKey
                existing.name = body.name?.trim() ?: existing.name
                existing.url = normalizedUrl ?: body.url?.trim().orEmpty()
                existing.format = body.format ?: existing.format ?: ChipFormatType.TEXT
                existing.prefix = body.prefix ?: existing.prefix.orEmpty()
                existing.color = body.color?.let { parseChipHexColor(it)?.toHex() } ?: existing.color ?: defaultChipHexColor
                existing.jsonPath = body.jsonPath?.trim() ?: existing.jsonPath.orEmpty()
                if (body.isAbove != null) existing.isAbove = body.isAbove
                if (body.order != null) existing.order = body.order

                val updated = db.update(existing)

                if (previousFeedKey != null && previousFeedKey != newFeedKey) {
                    chipService.cleanupFeedIfUnsubscribed(previousFeedKey)
                }

                chipService.notifyPersonWebTabs(me.id!!)

                PersonChipExtended(
                    chip = updated.apply { this.feed = null },
                    value = newFeed?.value,
                    syncedAt = newFeed?.syncedAt,
                )
            }
        }

        post("/chips/{id}/delete") {
            val personKey = me.id!!.asKey()
            val chipKey = parameter("id").asKey()
            respond {
                val existing = db.document(PersonChip::class, chipKey)
                    ?: return@respond HttpStatusCode.NotFound
                if (existing.person != personKey) {
                    return@respond HttpStatusCode.Forbidden
                }

                val oldFeedKey = existing.feed
                db.delete(existing)
                chipService.cleanupFeedIfUnsubscribed(oldFeedKey)
                chipService.notifyPersonWebTabs(me.id!!)

                HttpStatusCode.OK
            }
        }

        post("/chips/{id}/sync") {
            val personKey = me.id!!.asKey()
            val chipKey = parameter("id").asKey()
            respond {
                val existing = db.document(PersonChip::class, chipKey)
                    ?: return@respond HttpStatusCode.NotFound
                if (existing.person != personKey) {
                    return@respond HttpStatusCode.Forbidden
                }

                val normalizedUrl = chipService.normalizeUrl(existing.url)
                if (normalizedUrl != null) {
                    val feed = chipService.ensureAndSyncSubscribedFeed(normalizedUrl, force = true)
                    if (feed != null && existing.feed != feed.id?.asKey()) {
                        existing.feed = feed.id?.asKey()
                        db.update(existing)
                    }
                    return@respond PersonChipExtended(
                        chip = existing.apply { this.feed = null },
                        value = feed?.value,
                        syncedAt = feed?.syncedAt,
                    )
                }

                val currentFeed = existing.feed?.let { db.document(ChipFeed::class, it) }
                db.personChipExtended(personKey, chipKey) ?: PersonChipExtended(
                    chip = existing.apply { this.feed = null },
                    value = currentFeed?.value,
                    syncedAt = currentFeed?.syncedAt,
                )
            }
        }
    }
}
