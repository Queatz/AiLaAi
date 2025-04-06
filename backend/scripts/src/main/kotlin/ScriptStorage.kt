import kotlin.reflect.KClass

enum class ScriptStorageScope {
    Global,
    Local
}

interface ScriptStorage {
    operator fun invoke(scope: ScriptStorageScope = ScriptStorageScope.Local): ScriptStorage {
        return this
    }

    // Default collection accessors (Key / Value)
    operator fun get(key: String): String?
    operator fun set(key: String, value: String?)

    fun <T : Any> set(
        collection: KClass<T>,
        key: String,
        value: T,
    )

    fun <T : Any> get(
        collection: KClass<T>,
        key: String,
    )

    fun <T : Any> all(
        collection: KClass<T>,
    ): List<T>

    fun <T : Any> delete(
        collection: KClass<T>,
        key: String,
    )

    // General querying
    fun <T : Any> query(
        aql: String,
        type: KClass<T>,
        params: Map<String, Any?> = emptyMap()
    ): List<T>
}
