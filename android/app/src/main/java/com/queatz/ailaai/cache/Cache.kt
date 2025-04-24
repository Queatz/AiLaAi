package com.queatz.ailaai.cache

import com.queatz.ailaai.data.json
import com.queatz.ailaai.db.CacheDbModel
import com.queatz.ailaai.db.CacheDbModel_
import com.queatz.ailaai.db.db

val cache by lazy {
    Cache()
}

enum class CacheKey {
    Me,
    Groups
}

class Cache {
    inline fun <reified T : Any> put(key: CacheKey, value: T) {
        runCatching {
            db.box<CacheDbModel>().put(
                CacheDbModel(
                    key = key.name,
                    value = json.encodeToString(value)
                )
            )
        }
    }

    inline fun <reified T : Any> get(key: CacheKey): T? = runCatching {
        db.box<CacheDbModel>().query(CacheDbModel_.key.equal(key.name)).build().findFirst()?.let {
            json.decodeFromString<T>(it.value)
        }
    }.getOrNull()

    fun remove(key: CacheKey) {
        runCatching {
            db.box<CacheDbModel>().query(CacheDbModel_.key.equal(key.name)).build().remove()
        }
    }
}
