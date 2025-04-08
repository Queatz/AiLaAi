package com.queatz.scripts.store

import ScriptStorage
import ScriptStorageScope
import com.arangodb.ArangoCollection
import com.arangodb.ArangoDB
import com.arangodb.ContentType
import com.arangodb.entity.CollectionType
import com.arangodb.model.CollectionCreateOptions
import com.arangodb.model.DBCreateOptions
import com.arangodb.model.DatabaseUsersOptions
import com.arangodb.model.DocumentCreateOptions
import com.arangodb.model.OverwriteMode
import com.arangodb.serde.jackson.JacksonSerde
import com.arangodb.serde.jackson.Key
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.queatz.db.Db
import com.queatz.db.InstantModule
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.reflect.KClass

private const val DEFAULT_COLLECTION = "default"

@OptIn(ExperimentalSerializationApi::class)
@Serializable
open class StorageModel {
    @Key
    @JsonAlias("key")
    @JsonNames("key", "_key")
    @JsonProperty("_key")
    var key: String? = null
}

@Serializable
class KeyValueModel : StorageModel() {
    var value: String? = null
    var createdAt: Instant? = null
}

class ArangoScriptStorage(
    scriptOwner: String,
    person: String?,
    scope: ScriptStorageScope = ScriptStorageScope.Global,
) : ScriptStorage {

    private val db by lazy {
        ArangoDB.Builder()
            .host("127.0.0.1", 8529)
            .user(Db.USER)
            .password(Db.PASSWORD)
            .serde(
                JacksonSerde.of(ContentType.JSON).apply {
                    configure {
                        it.registerModule(InstantModule())
                        it.registerModule(KotlinModule.Builder().build())
                    }
                }
            )
            .build().run {
                val dbName = "script_$scriptOwner${if (scope == ScriptStorageScope.Local) "_$person" else ""}"

                if (dbName !in databases) {
                    createDatabase(
                        DBCreateOptions()
                            .name(dbName)
                            .users(
                                listOf(DatabaseUsersOptions().username(Db.USER))
                            )
                    )
                }

                db(dbName)
            }
    }

    override fun get(key: String): String? {
        return collection(DEFAULT_COLLECTION)
            .getDocument(key, KeyValueModel::class.java)
            ?.value
    }

    override fun set(key: String, value: String?) {
        collection(DEFAULT_COLLECTION).insertDocument(
            KeyValueModel().apply {
                this.key = key
                this.value = value
                this.createdAt = Clock.System.now()
            },
            DocumentCreateOptions()
                .overwriteMode(OverwriteMode.replace)
        )
    }

    override fun <T : Any> get(collection: KClass<T>, key: String): T? {
        if (!hasCollection(collectionName(collection))) {
            return null
        }

        return collection(collectionName(collection))
            .getDocument(key, collection.java)
    }

    override fun <T : Any> put(collection: KClass<T>, value: T): T {
        return collection(collectionName(collection))
            .insertDocument(
                value,
                DocumentCreateOptions()
                    .overwriteMode(OverwriteMode.replace)
                    .returnNew(true)
            ).new!!
    }

    override fun <T : Any> all(collection: KClass<T>): List<T> {
        if (!hasCollection(collectionName(collection))) {
            return emptyList()
        }
        return query(
            aql = "FOR document IN @@collection RETURN document",
            type = collection,
            params = mapOf("@collection" to collectionName(collection))
        )
    }

    override fun <T : Any> delete(collection: KClass<T>, key: String) {
        collection(collectionName(collection))
            .deleteDocument(key)
    }

    override fun <T : Any> query(
        type: KClass<T>,
        aql: String,
        params: Map<String, Any?>,
    ): List<T> {
        return synchronized(db) {
            db.query(
                aql,
                type.java,
                params
            ).asListRemaining()
        }
    }

    private fun collectionName(collection: KClass<*>): String = collection.qualifiedName!!.replace(".", "_")

    private fun hasCollection(name: String): Boolean = db.collections.any { it.name == name }

    private fun collection(name: String): ArangoCollection {
        if (!hasCollection(name)) {
            db.createCollection(
                name,
                CollectionCreateOptions().type(CollectionType.DOCUMENT)
            )
        }

        return db.collection(name)
    }
}
