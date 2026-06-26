package com.queatz

import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.notify
import com.queatz.plugins.openAi
import com.queatz.push.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.logging.Logger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

val signalService = SignalService()

/**
 * Service to handle background processing for Signals.
 * 
 * This includes:
 * - Bucketing: Collecting replies every 5 minutes and ranking them by affinity.
 * - Staggered release: Releasing ranked replies one by one with a 1-minute buffer.
 */
class SignalService {
    private lateinit var scope: CoroutineScope
    private val isDev = true // System.getenv("DEV") == "true"

    /**
     * Start the background processing loops.
     */
    fun start(coroutineScope: CoroutineScope) {
        this.scope = coroutineScope

        // Staggered release: check every 30 seconds
        scope.launch {
            val checkInterval = 30.seconds
            Logger.getAnonymousLogger().info("[SIGNALS] Starting releaseReplies loop (interval: $checkInterval)")
            while (true) {
                runCatching {
                    releaseReplies()
                }.onFailure {
                    Logger.getAnonymousLogger().severe("[SIGNALS] releaseReplies error: ${it.message}")
                    it.printStackTrace()
                }
                delay(checkInterval)
            }
        }

        // Bucketing: check at bucket intervals
        scope.launch {
            val bucketInterval = if (isDev) 15.seconds else 5.minutes
            Logger.getAnonymousLogger().info("[SIGNALS] Starting processBuckets loop (interval: $bucketInterval)")
            while (true) {
                runCatching {
                    processBuckets()
                }.onFailure {
                    Logger.getAnonymousLogger().severe("[SIGNALS] processBuckets error: ${it.message}")
                    it.printStackTrace()
                }
                delay(bucketInterval)
            }
        }
    }

    /**
     * Check for replies that are scheduled to be released and send notifications.
     */
    private suspend fun releaseReplies() {
        val now = Clock.System.now()
        val unreleased = db.unreleasedSignalReplies(now)
        if (unreleased.isNotEmpty()) {
            Logger.getAnonymousLogger().info("[SIGNALS] releaseReplies found ${unreleased.size} unreleased replies at $now")
        }
        unreleased.forEach { reply ->
            val signalSend = db.document(SignalSend::class, reply.signalSend!!) ?: return@forEach
            // Check if expired
            if (signalSend.expiry!! <= now) {
                // Discard
                Logger.getAnonymousLogger().info("[SIGNALS] discarding expired reply ${reply.id} for signalSend ${signalSend.id}")
                db.delete(reply)
                return@forEach
            }

            // Release
            Logger.getAnonymousLogger().info("[SIGNALS] releasing reply ${reply.id} for signalSend ${signalSend.id}")
            reply.released = true
            db.update(reply)

            // Notify
            val person = db.document(Person::class, reply.person!!) ?: return@forEach
            val signal = db.document(Signal::class, signalSend.signal!!) ?: return@forEach
            
            Logger.getAnonymousLogger().info("[SIGNALS] notifyPeople for reply ${reply.id} to ${signalSend.person}")

            val transcription = reply.audio?.let { audioPath ->
                try {
                    val file = File(".$audioPath")
                    if (file.exists()) {
                        openAi.transcribe(file.readBytes())
                    } else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            notify.notifyPeople(
                people = listOf(signalSend.person!!),
                pushData = PushData(
                    action = PushAction.SignalReply,
                    data = SignalReplyPushData(
                        person = person,
                        signal = signal,
                        signalSend = signalSend,
                        signalReply = reply,
                        transcription = transcription
                    )
                )
            )
        }
    }

    /**
     * Group unprocessed replies into 5-minute buckets, rank them by affinity,
     * and schedule their release times.
     */
    private suspend fun processBuckets() {
        val now = Clock.System.now()
        val bufferInterval = if (isDev) 5.seconds else 1.minutes
        val bucketEnd = now
        val unprocessed = db.unprocessedSignalReplies(bucketEnd)
        
        if (unprocessed.isNotEmpty()) {
            Logger.getAnonymousLogger().info("[SIGNALS] processBuckets found ${unprocessed.size} unprocessed replies at $now")
        }
        
        val groupedBySend = unprocessed.groupBy { it.signalSend }
        
        groupedBySend.forEach { (sendId, replies) ->
            val signalSend = db.document(SignalSend::class, sendId!!) ?: return@forEach
            if (signalSend.expiry!! <= now) {
                 Logger.getAnonymousLogger().info("[SIGNALS] processBuckets discarding ${replies.size} replies for expired signalSend $sendId")
                 replies.forEach { db.delete(it) }
                 return@forEach
            }
            
            Logger.getAnonymousLogger().info("[SIGNALS] processBuckets for signalSend $sendId: ${replies.size} replies")
            
            val senderSignals = db.personSignals(signalSend.person!!).filter { it.turnedOn == true }.mapNotNull { it.signal }.toSet()
            Logger.getAnonymousLogger().info("[SIGNALS] sender ${signalSend.person} signals: $senderSignals")
            
            val rankedReplies = replies.map { reply ->
                val responderSignals = db.personSignals(reply.person!!).filter { it.turnedOn == true }.mapNotNull { it.signal }.toSet()
                val affinity = senderSignals.intersect(responderSignals).size
                Logger.getAnonymousLogger().info("[SIGNALS] reply ${reply.id} from ${reply.person} affinity: $affinity (signals: $responderSignals)")
                reply to affinity
            }.sortedByDescending { it.second }
            
            // Assign release times
            // Find the latest releaseAt for this signalSend
            // Actually, we can just start from now + bufferInterval
            var nextRelease = now + bufferInterval
            rankedReplies.forEach { (reply, _) ->
                Logger.getAnonymousLogger().info("[SIGNALS] assigned release time to reply ${reply.id}: $nextRelease")
                reply.releaseAt = nextRelease
                db.update(reply)
                nextRelease += bufferInterval
            }
        }
    }
}
