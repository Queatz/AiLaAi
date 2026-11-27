package com.queatz

import com.queatz.db.ChipFeed
import com.queatz.db.ChipPreviewResponse
import com.queatz.db.DeviceType
import com.queatz.db.asKey
import com.queatz.db.chipFeedByUrl
import com.queatz.db.chipSubscribers
import com.queatz.db.countChipSubscribers
import com.queatz.db.peopleDevices
import com.queatz.db.upsertChipFeed
import com.queatz.plugins.db
import com.queatz.plugins.push
import com.queatz.push.ChipPushData
import com.queatz.push.PushAction
import com.queatz.push.PushData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

val chipService = ChipService()

/**
 * Global URL-deduplicated, subscriber-backed Chip sync service.
 *
 * Strictly triggered on-demand (no background polling loop):
 * - Automatic refresh when any subscriber loads chips and the shared ChipFeed is older than [autoRefreshTtl].
 * - Manual "Sync Now" refresh with a global per-URL cooldown ([manualSyncCooldown]) so thousands of users
 *   syncing the same popular URL only execute a single upstream HTTP GET globally and receive the exact same
 *   cached [ChipFeed.value] and [ChipFeed.syncedAt].
 * - Zero cross-user leakage of URLs or subscriber identities.
 */
class ChipService {
    private lateinit var scope: CoroutineScope

    private val autoRefreshTtl = 1.hours
    private val manualSyncCooldown = 60.seconds
    private val failureRetryCooldown = 1.minutes
    private val maxResponseBytes = 256 * 1024 // 256 KB cap
    private val maxRedirects = 5
    private val timeoutMs = 15_000

    private val inFlightFeeds = ConcurrentHashMap<String, Deferred<ChipFeed>>()
    private val inFlightPreviews = ConcurrentHashMap<String, Deferred<ChipPreviewResponse>>()
    private val previewCache = ConcurrentHashMap<String, ChipPreviewResponse>()

    fun start(
        coroutineScope: CoroutineScope,
    ) {
        this.scope = coroutineScope
    }

    fun normalizeUrl(
        rawUrl: String?,
    ): String? {
        val trimmed = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return trimmed
    }

    fun isFeedStaleForAutoRefresh(
        feed: ChipFeed,
        now: Instant = Clock.System.now(),
    ): Boolean {
        val syncedAt = feed.syncedAt
        if (syncedAt != null && now - syncedAt < autoRefreshTtl) {
            return false
        }
        val attemptedAt = feed.attemptedAt
        if (attemptedAt != null && now - attemptedAt < failureRetryCooldown) {
            return false
        }
        return true
    }

    fun canManualSync(
        feed: ChipFeed,
        now: Instant = Clock.System.now(),
    ): Boolean {
        val syncedAt = feed.syncedAt ?: return true
        return now - syncedAt >= manualSyncCooldown
    }

    /**
     * Ensures a [ChipFeed] exists for [rawUrl] and refreshes it on-demand if needed.
     */
    suspend fun ensureAndSyncSubscribedFeed(
        rawUrl: String?,
        force: Boolean = false,
    ): ChipFeed? {
        val normalized = normalizeUrl(rawUrl) ?: return null
        val existing = db.upsertChipFeed(normalized)
        val now = Clock.System.now()

        val shouldFetch = if (force) {
            canManualSync(existing, now)
        } else {
            isFeedStaleForAutoRefresh(existing, now)
        }

        if (!shouldFetch) {
            return existing
        }

        val deferred = CompletableDeferred<ChipFeed>()
        val active = inFlightFeeds.putIfAbsent(normalized, deferred)
        if (active != null) {
            return active.await()
        }

        return try {
            val latest = db.upsertChipFeed(normalized)
            val recheckNow = Clock.System.now()
            val stillShouldFetch = if (force) {
                canManualSync(latest, recheckNow)
            } else {
                isFeedStaleForAutoRefresh(latest, recheckNow)
            }

            if (!stillShouldFetch) {
                deferred.complete(latest)
                return latest
            }

            val fetchResult = fetchExternalUrlSafe(normalized)
            val finishTime = Clock.System.now()
            latest.attemptedAt = finishTime
            if (fetchResult != null) {
                latest.value = fetchResult.body
                latest.statusCode = fetchResult.statusCode
                latest.syncedAt = finishTime
            }
            val saved = db.update(latest)
            deferred.complete(saved)

            if (fetchResult != null && saved.id != null) {
                notifyFeedSubscribers(saved.id!!, saved.syncedAt)
            }

            saved
        } catch (t: Throwable) {
            Logger.getAnonymousLogger().warning("[CHIPS] Sync error for feed: ${t.message}")
            deferred.complete(existing)
            existing
        } finally {
            inFlightFeeds.remove(normalized, deferred)
        }
    }

    /**
     * Previews a URL (e.g. from a shared ChipAttachment message) without persisting unsubscribed URLs to ArangoDB.
     * If at least one subscriber already has this URL installed, shares the global [ChipFeed] cache & timestamp.
     */
    suspend fun previewUrl(
        rawUrl: String?,
    ): ChipPreviewResponse {
        val normalized = normalizeUrl(rawUrl) ?: return ChipPreviewResponse()

        // If already subscribed in DB, reuse the global ChipFeed!
        val subscribedFeed = db.chipFeedByUrl(normalized)
        if (subscribedFeed != null) {
            val refreshed = ensureAndSyncSubscribedFeed(normalized, force = false) ?: subscribedFeed
            return ChipPreviewResponse(
                value = refreshed.value,
                syncedAt = refreshed.syncedAt,
            )
        }

        val now = Clock.System.now()
        previewCache[normalized]?.let { cached ->
            if (cached.syncedAt != null && now - cached.syncedAt!! < manualSyncCooldown) {
                return cached
            }
        }

        val deferred = CompletableDeferred<ChipPreviewResponse>()
        val active = inFlightPreviews.putIfAbsent(normalized, deferred)
        if (active != null) {
            return active.await()
        }

        return try {
            val fetched = fetchExternalUrlSafe(normalized)
            val response = if (fetched != null) {
                ChipPreviewResponse(
                    value = fetched.body,
                    syncedAt = Clock.System.now(),
                ).also {
                    if (previewCache.size > 256) {
                        previewCache.clear()
                    }
                    previewCache[normalized] = it
                }
            } else {
                ChipPreviewResponse()
            }
            deferred.complete(response)
            response
        } catch (_: Throwable) {
            val empty = ChipPreviewResponse()
            deferred.complete(empty)
            empty
        } finally {
            inFlightPreviews.remove(normalized, deferred)
        }
    }

    /**
     * Removes a [ChipFeed] from ArangoDB if it no longer has any subscribers.
     */
    fun cleanupFeedIfUnsubscribed(
        feedKey: String?,
    ) {
        val key = feedKey?.asKey()?.takeIf { it.isNotBlank() } ?: return
        if (db.countChipSubscribers(key) == 0) {
            db.document(ChipFeed::class, key)?.let { feed ->
                runCatching { db.delete(feed) }
            }
        }
    }

    fun notifyPersonWebTabs(
        personId: String,
        syncedAt: Instant? = Clock.System.now(),
    ) {
        if (!::scope.isInitialized) return
        scope.launch(Dispatchers.IO) {
            sendWebSseToPeople(
                people = listOf(personId),
                syncedAt = syncedAt,
            )
        }
    }

    private fun notifyFeedSubscribers(
        feedId: String,
        syncedAt: Instant?,
    ) {
        if (!::scope.isInitialized) return
        scope.launch(Dispatchers.IO) {
            val subscribers = db.chipSubscribers(feedId)
            if (subscribers.isNotEmpty()) {
                sendWebSseToPeople(
                    people = subscribers,
                    syncedAt = syncedAt,
                )
            }
        }
    }

    private fun sendWebSseToPeople(
        people: List<String>,
        syncedAt: Instant?,
    ) {
        if (people.isEmpty()) return
        val pushData = PushData(
            action = PushAction.Chip,
            data = ChipPushData(syncedAt = syncedAt),
        )
        db.peopleDevices(people)
            .filter { it.type == DeviceType.Web }
            .forEach { device ->
                push.sendPush(device, pushData)
            }
    }

    private data class FetchOutcome(
        val statusCode: Int,
        val body: String,
    )

    private suspend fun fetchExternalUrlSafe(
        initialUrl: String,
    ): FetchOutcome? = withContext(Dispatchers.IO) {
        var currentUrl = initialUrl
        for (hop in 0..maxRedirects) {
            val uri = runCatching { URI(currentUrl) }.getOrNull() ?: return@withContext null
            if (!isSafeExternalUri(uri)) {
                Logger.getAnonymousLogger().warning("[CHIPS] Blocked unsafe URL: $currentUrl")
                return@withContext null
            }

            val connection = (uri.toURL().openConnection() as? HttpURLConnection) ?: return@withContext null
            try {
                connection.instanceFollowRedirects = false
                connection.requestMethod = "GET"
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                connection.setRequestProperty(
                    "Accept",
                    "application/json,text/plain,text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                )
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                connection.setRequestProperty("Cache-Control", "max-age=0")
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (compatible; AiLaAi-Chips/1.0)",
                )

                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")?.takeIf { it.isNotBlank() }
                        ?: return@withContext null
                    currentUrl = uri.resolve(location).toString()
                    continue
                }

                if (code in 200..299) {
                    val body = connection.inputStream.use { stream ->
                        val buffer = ByteArray(8192)
                        val out = ByteArrayOutputStream()
                        var total = 0
                        while (true) {
                            val read = stream.read(buffer)
                            if (read <= 0) break
                            val toWrite = read.coerceAtMost(maxResponseBytes - total)
                            if (toWrite > 0) {
                                out.write(buffer, 0, toWrite)
                                total += toWrite
                            }
                            if (total >= maxResponseBytes) break
                        }
                        out.toString(StandardCharsets.UTF_8).trim()
                    }
                    return@withContext FetchOutcome(
                        statusCode = code,
                        body = body,
                    )
                } else {
                    return@withContext null
                }
            } finally {
                connection.disconnect()
            }
        }
        null
    }

    private fun isSafeExternalUri(
        uri: URI,
    ): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase()?.trim('[', ']') ?: return false
        if (host.isBlank() || host == "localhost" || host.endsWith(".localhost") || host.endsWith(".internal")) {
            return false
        }

        val addresses = runCatching { InetAddress.getAllByName(host) }.getOrNull() ?: return false
        if (addresses.isEmpty()) return false

        return addresses.all { addr ->
            !addr.isAnyLocalAddress &&
                !addr.isLoopbackAddress &&
                !addr.isLinkLocalAddress &&
                !addr.isSiteLocalAddress &&
                !addr.isMulticastAddress &&
                !isPrivateOrReservedIp(addr)
        }
    }

    private fun isPrivateOrReservedIp(
        addr: InetAddress,
    ): Boolean {
        val bytes = addr.address
        if (bytes.size == 4) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            // 0.0.0.0/8, 100.64.0.0/10 (CGNAT), 127.0.0.0/8, 169.254.0.0/16
            if (b0 == 0 || b0 == 127) return true
            if (b0 == 100 && (b1 in 64..127)) return true
            if (b0 == 169 && b1 == 254) return true
            if (b0 >= 224) return true
        } else if (addr is Inet6Address && bytes.size == 16) {
            val b0 = bytes[0].toInt() and 0xFF
            // fc00::/7 Unique Local IPv6
            if ((b0 and 0xFE) == 0xFC) return true
        }
        return false
    }
}
