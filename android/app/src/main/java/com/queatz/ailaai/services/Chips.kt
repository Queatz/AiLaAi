package com.queatz.ailaai.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.queatz.ailaai.Application
import com.queatz.ailaai.dataStore
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours
import kotlin.time.Clock
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*

import kotlinx.serialization.json.*

@Serializable
enum class ChipFormatType {
    TEXT,
    NUMBER,
    FIXED_2,
    JSON
}

@Serializable
data class ChipConfig(
    val id: String,
    val name: String = "",
    val url: String = "",
    val type: ChipFormatType = ChipFormatType.TEXT,
    val prefix: String = "",
    val backgroundColor: Int = 0,
    val isAbove: Boolean = true,
    val jsonPath: String = ""
)

sealed interface JsonParsedResult {
    data class BooleanValue(val value: Boolean) : JsonParsedResult
    data object NullValue : JsonParsedResult
    data object UndefinedValue : JsonParsedResult
    data class TextValue(val value: String) : JsonParsedResult
}

private val jsonParser = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun evaluateJson(rawString: String, jsonPath: String): JsonParsedResult {
    val trimmed = rawString.trim()
    if (trimmed.isEmpty()) return JsonParsedResult.UndefinedValue

    val root = try {
        jsonParser.parseToJsonElement(trimmed)
    } catch (_: Exception) {
        return JsonParsedResult.UndefinedValue
    }

    val segments = jsonPath
        .replace('[', '.')
        .replace(']', '.')
        .split('.')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    var current: JsonElement? = root
    for (segment in segments) {
        if (current == null) break
        current = when (current) {
            is JsonObject -> current[segment]
            is JsonArray -> {
                val index = segment.toIntOrNull()
                if (index != null && index in current.indices) {
                    current[index]
                } else {
                    null
                }
            }
            else -> null
        }
    }

    if (current == null) {
        return JsonParsedResult.UndefinedValue
    }

    return when (current) {
        is JsonNull -> JsonParsedResult.NullValue
        is JsonPrimitive -> {
            val b = current.booleanOrNull
            if (!current.isString && b != null) {
                JsonParsedResult.BooleanValue(b)
            } else if (current.content.equals("true", ignoreCase = true)) {
                JsonParsedResult.BooleanValue(true)
            } else if (current.content.equals("false", ignoreCase = true)) {
                JsonParsedResult.BooleanValue(false)
            } else {
                JsonParsedResult.TextValue(current.content)
            }
        }
        is JsonObject, is JsonArray -> {
            JsonParsedResult.TextValue(current.toString())
        }
    }
}

@Serializable
data class ChipCache(
    val value: String = "",
    val timestamp: Long = 0
)

val chips by lazy { Chips() }

class Chips {
    private lateinit var context: Context
    private val scope = CoroutineScope(Dispatchers.IO)
    private val configKey = stringPreferencesKey("app.chips_config")
    private val cacheKey = stringPreferencesKey("app.chips_cache")

    private val _configs = MutableStateFlow<List<ChipConfig>>(emptyList())
    val configs: StateFlow<List<ChipConfig>> = _configs.asStateFlow()

    private val _cache = MutableStateFlow<Map<String, ChipCache>>(emptyMap())
    val cache: StateFlow<Map<String, ChipCache>> = _cache.asStateFlow()

    private val httpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
        }
    }

    fun init(context: Context) {
        this.context = context
        scope.launch {
            val configString = context.dataStore.data.first()[configKey]
            if (!configString.isNullOrBlank()) {
                try {
                    _configs.value = jsonParser.decodeFromString(configString)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val cacheString = context.dataStore.data.first()[cacheKey]
            if (!cacheString.isNullOrBlank()) {
                try {
                    _cache.value = jsonParser.decodeFromString(cacheString)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            refreshCacheIfNeeded()
        }
    }

    fun addConfig(config: ChipConfig) {
        val newConfigs = _configs.value + config
        updateConfigs(newConfigs)
    }

    fun removeConfig(id: String) {
        val newConfigs = _configs.value.filter { it.id != id }
        updateConfigs(newConfigs)

        val newCache = _cache.value.toMutableMap()
        newCache.remove(id)
        updateCache(newCache)
    }

    fun updateConfig(config: ChipConfig) {
        val newConfigs = _configs.value.map { if (it.id == config.id) config else it }
        updateConfigs(newConfigs)

        // Refresh this chip specifically
        scope.launch {
            fetchChipData(config)
        }
    }

    private fun updateConfigs(newConfigs: List<ChipConfig>) {
        _configs.value = newConfigs
        scope.launch {
            context.dataStore.edit {
                it[configKey] = jsonParser.encodeToString(newConfigs)
            }
        }
    }

    private fun updateCache(newCache: Map<String, ChipCache>) {
        _cache.value = newCache
        scope.launch {
            context.dataStore.edit {
                it[cacheKey] = jsonParser.encodeToString(newCache)
            }
        }
    }

    fun refreshCacheIfNeeded() {
        scope.launch {
            val now = System.currentTimeMillis()
            val oneHourAgo = now - 1.hours.inWholeMilliseconds

            _configs.value.forEach { config ->
                val cached = _cache.value[config.id]
                if (cached == null || cached.timestamp < oneHourAgo) {
                    fetchChipData(config)
                }
            }
        }
    }
    
    suspend fun syncChip(config: ChipConfig): Boolean {
        return fetchChipData(config)
    }

    private suspend fun fetchChipData(config: ChipConfig): Boolean {
        if (config.url.isBlank()) return false
        try {
            val response: HttpResponse = httpClient.get(config.url) {
                header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/jxl,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                header("Accept-Language", "en-US,en;q=0.9")
                header("Cache-Control", "max-age=0")
                header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/158.0.0.0 Safari/537.36")
                header("sec-ch-ua", "\"Chromium\";v=\"158\", \"Not.A/Brand\";v=\"24\", \"Google Chrome\";v=\"158\"")
                header("sec-ch-ua-mobile", "?0")
                header("sec-ch-ua-platform", "\"Linux\"")
            }
            if (response.status.value in 200..299) {
                val value = response.bodyAsText().trim()
                val newCache = _cache.value.toMutableMap()
                newCache[config.id] = ChipCache(
                    value = value,
                    timestamp = System.currentTimeMillis()
                )
                updateCache(newCache)
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
