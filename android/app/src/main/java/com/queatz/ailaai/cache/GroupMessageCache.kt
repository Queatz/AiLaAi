package com.queatz.ailaai.cache

import android.content.Context
import android.util.LruCache
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.queatz.ailaai.data.json
import com.queatz.ailaai.dataStore
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val MAX_CACHED_MESSAGES_PER_GROUP = 100
const val MAX_MESSAGE_CACHE_BYTES = 256L * 1024 * 1024 // 256 MB (paired with 768 MB image cache = 1 GB total)
const val MAX_IMAGE_CACHE_BYTES = 768L * 1024 * 1024
private const val MAX_IN_MEMORY_GROUPS = 20
private const val MESSAGE_CACHE_DIR_NAME = "group_messages"

private val messageCacheEnabledKey = booleanPreferencesKey("cache.messages.enabled")

val groupMessageCache by lazy {
    GroupMessageCache()
}

@Serializable
data class CachedGroupData(
    val group: GroupExtended? = null,
    val messages: List<Message> = emptyList(),
    val hasOlderMessages: Boolean = true,
)

data class CachedGroupEntry(
    val groupId: String,
    val group: GroupExtended?,
    val messageCount: Int,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
)

data class ClearCacheFailure(
    val id: String,
    val name: String,
    val reason: String,
)

data class ClearCacheResult(
    val clearedCount: Int,
    val freedBytes: Long,
    val failures: List<ClearCacheFailure>,
)

/**
 * Merges a freshly fetched page of recent messages (sorted newest-first) with previously cached messages.
 *
 * - If [fresh] is empty, all messages are cleared.
 * - If [fresh] overlaps or is contiguous with [cached] (`fresh.last().createdAt <= cached.first().createdAt`),
 *   [fresh] is authoritative for `[fresh.last().createdAt .. now]` (reflecting edits, reaction updates,
 *   and deletions in that range), while strictly older cached messages are retained up to [maxCount].
 * - If there is a gap (`fresh.last().createdAt > cached.first().createdAt`), stale older cached messages
 *   are discarded to avoid displaying a silent gap of missing messages.
 */
fun mergeCachedMessages(
    cached: List<Message>,
    fresh: List<Message>,
    maxCount: Int = MAX_CACHED_MESSAGES_PER_GROUP,
): List<Message> {
    if (fresh.isEmpty()) {
        return emptyList()
    }

    val normalizedFresh = fresh.distinctBy { it.id }

    if (cached.isEmpty()) {
        return normalizedFresh.take(maxCount)
    }

    val oldestFresh = normalizedFresh.lastOrNull()?.createdAt
    val newestCached = cached.firstOrNull()?.createdAt

    if (oldestFresh == null || newestCached == null || oldestFresh > newestCached) {
        return normalizedFresh.take(maxCount)
    }

    val olderCached = cached.filter { message ->
        val createdAt = message.createdAt
        createdAt != null && createdAt < oldestFresh
    }

    return (normalizedFresh + olderCached)
        .distinctBy { it.id }
        .take(maxCount)
}

class GroupMessageCache {
    private lateinit var context: Context
    private lateinit var cacheDir: File

    private val memoryCache = LruCache<String, CachedGroupData>(MAX_IN_MEMORY_GROUPS)
    private val diskMutex = Mutex()

    private val _enabled = MutableStateFlow(true)
    val enabled = _enabled.asStateFlow()

    fun init(context: Context) {
        this.context = context.applicationContext
        this.cacheDir = File(this.context.cacheDir, MESSAGE_CACHE_DIR_NAME)

        val initialEnabled = runCatching {
            runBlocking {
                this@GroupMessageCache.context.dataStore.data.first()[messageCacheEnabledKey] ?: true
            }
        }.getOrDefault(true)

        _enabled.value = initialEnabled
    }

    fun isInitialized(): Boolean = ::cacheDir.isInitialized

    fun getInMemory(groupId: String): CachedGroupData? {
        if (!_enabled.value) {
            return null
        }
        return memoryCache.get(groupId)
    }

    suspend fun get(groupId: String): CachedGroupData? {
        if (!_enabled.value || !isInitialized()) {
            return null
        }

        memoryCache.get(groupId)?.let { return it }

        return withContext(Dispatchers.IO) {
            val file = fileFor(groupId)
            if (!file.exists()) {
                return@withContext null
            }

            runCatching {
                val data = json.decodeFromString<CachedGroupData>(file.readText())
                file.setLastModified(System.currentTimeMillis())
                memoryCache.put(groupId, data)
                data
            }.onFailure {
                runCatching { file.delete() }
            }.getOrNull()
        }
    }

    suspend fun putMessages(
        groupId: String,
        group: GroupExtended? = null,
        messages: List<Message>,
        hasOlderMessages: Boolean = true,
    ) {
        if (!_enabled.value || !isInitialized()) {
            return
        }

        val trimmed = messages.distinctBy { it.id }.take(MAX_CACHED_MESSAGES_PER_GROUP)

        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                if (!_enabled.value) {
                    return@withLock
                }

                val existing = memoryCache.get(groupId) ?: readFromDiskUnchecked(groupId)
                val updated = CachedGroupData(
                    group = group ?: existing?.group,
                    messages = trimmed,
                    hasOlderMessages = hasOlderMessages,
                )

                memoryCache.put(groupId, updated)
                writeToDiskUnchecked(
                    groupId = groupId,
                    data = updated,
                )
                pruneIfOverBudgetUnchecked()
            }
        }
    }

    suspend fun putGroup(
        groupId: String,
        group: GroupExtended,
    ) {
        if (!_enabled.value || !isInitialized()) {
            return
        }

        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                if (!_enabled.value) {
                    return@withLock
                }

                val existing = memoryCache.get(groupId) ?: readFromDiskUnchecked(groupId)
                val updated = (existing ?: CachedGroupData()).copy(group = group)
                memoryCache.put(groupId, updated)
                writeToDiskUnchecked(
                    groupId = groupId,
                    data = updated,
                )
                pruneIfOverBudgetUnchecked()
            }
        }
    }

    suspend fun remove(groupId: String): Result<Long> {
        memoryCache.remove(groupId)

        if (!isInitialized()) {
            return Result.success(0L)
        }

        return withContext(Dispatchers.IO) {
            diskMutex.withLock {
                removeFileUnchecked(groupId)
            }
        }
    }

    suspend fun removeMultiple(
        groupIds: Collection<String>,
        resolveName: (String) -> String,
    ): ClearCacheResult {
        if (!isInitialized()) {
            return ClearCacheResult(
                clearedCount = 0,
                freedBytes = 0L,
                failures = emptyList(),
            )
        }

        return withContext(Dispatchers.IO) {
            diskMutex.withLock {
                var clearedCount = 0
                var freedBytes = 0L
                val failures = mutableListOf<ClearCacheFailure>()

                groupIds.forEach { groupId ->
                    memoryCache.remove(groupId)
                    removeFileUnchecked(groupId)
                        .onSuccess { bytes ->
                            clearedCount++
                            freedBytes += bytes
                        }
                        .onFailure { error ->
                            failures += ClearCacheFailure(
                                id = groupId,
                                name = resolveName(groupId),
                                reason = error.localizedMessage ?: error.javaClass.simpleName,
                            )
                        }
                }

                ClearCacheResult(
                    clearedCount = clearedCount,
                    freedBytes = freedBytes,
                    failures = failures,
                )
            }
        }
    }

    suspend fun listEntries(): Result<List<CachedGroupEntry>> {
        if (!isInitialized()) {
            return Result.success(emptyList())
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                diskMutex.withLock {
                    if (!cacheDir.exists()) {
                        return@withLock emptyList()
                    }

                    val files = cacheDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".json")
                    } ?: return@withLock emptyList()

                    files.mapNotNull { file ->
                        val groupId = decodeGroupId(file.name.removeSuffix(".json")) ?: return@mapNotNull null
                        val parsed = runCatching {
                            json.decodeFromString<CachedGroupData>(file.readText())
                        }.getOrNull()

                        CachedGroupEntry(
                            groupId = groupId,
                            group = parsed?.group,
                            messageCount = parsed?.messages?.size ?: 0,
                            sizeBytes = file.length(),
                            lastModifiedMillis = file.lastModified(),
                        )
                    }.sortedByDescending { it.sizeBytes }
                }
            }
        }
    }

    suspend fun sizeBytes(): Long {
        if (!isInitialized()) {
            return 0L
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                if (!cacheDir.exists()) {
                    return@runCatching 0L
                }
                cacheDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".json")
                }?.sumOf { it.length() } ?: 0L
            }.getOrDefault(0L)
        }
    }

    suspend fun clear(): Result<Long> {
        memoryCache.evictAll()

        if (!isInitialized()) {
            return Result.success(0L)
        }

        return withContext(Dispatchers.IO) {
            diskMutex.withLock {
                runCatching {
                    if (!cacheDir.exists()) {
                        return@runCatching 0L
                    }

                    var freed = 0L
                    cacheDir.listFiles()?.forEach { file ->
                        val size = if (file.isFile) file.length() else 0L
                        if (file.delete()) {
                            freed += size
                        }
                    }
                    freed
                }
            }
        }
    }

    suspend fun setEnabled(enabled: Boolean): Result<Unit> {
        _enabled.value = enabled

        if (!isInitialized()) {
            return Result.success(Unit)
        }

        val persistResult = runCatching {
            context.dataStore.edit { preferences ->
                preferences[messageCacheEnabledKey] = enabled
            }
            Unit
        }

        if (!enabled) {
            clear()
        }

        return persistResult
    }

    private fun fileFor(groupId: String): File {
        val encoded = URLEncoder.encode(groupId, StandardCharsets.UTF_8.name())
        return File(cacheDir, "$encoded.json")
    }

    private fun decodeGroupId(encoded: String): String? = runCatching {
        URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
    }.getOrNull()

    private fun readFromDiskUnchecked(groupId: String): CachedGroupData? {
        val file = fileFor(groupId)
        if (!file.exists()) {
            return null
        }

        return runCatching {
            json.decodeFromString<CachedGroupData>(file.readText())
        }.getOrNull()
    }

    private fun writeToDiskUnchecked(
        groupId: String,
        data: CachedGroupData,
    ) {
        runCatching {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val target = fileFor(groupId)
            val encoded = json.encodeToString(data)
            val temp = File(cacheDir, "${target.name}.${System.nanoTime()}.tmp")

            temp.writeText(encoded)
            if (!temp.renameTo(target)) {
                target.writeText(encoded)
                temp.delete()
            }
            target.setLastModified(System.currentTimeMillis())
        }
    }

    private fun removeFileUnchecked(groupId: String): Result<Long> = runCatching {
        val file = fileFor(groupId)
        if (!file.exists()) {
            return@runCatching 0L
        }

        val size = file.length()
        if (!file.delete() && file.exists()) {
            throw IllegalStateException("Could not delete cached file (${file.name})")
        }
        size
    }

    private fun pruneIfOverBudgetUnchecked() {
        runCatching {
            if (!cacheDir.exists()) {
                return
            }

            val files = cacheDir.listFiles { file ->
                file.isFile && file.name.endsWith(".json")
            } ?: return

            var totalBytes = files.sumOf { it.length() }
            if (totalBytes <= MAX_MESSAGE_CACHE_BYTES) {
                return
            }

            val oldestFirst = files.sortedBy { it.lastModified() }
            for (file in oldestFirst) {
                if (totalBytes <= MAX_MESSAGE_CACHE_BYTES) {
                    break
                }
                val fileSize = file.length()
                val groupId = decodeGroupId(file.name.removeSuffix(".json"))
                if (file.delete()) {
                    totalBytes -= fileSize
                    if (groupId != null) {
                        memoryCache.remove(groupId)
                    }
                }
            }
        }
    }
}
