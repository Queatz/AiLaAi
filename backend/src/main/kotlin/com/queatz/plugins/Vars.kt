package com.queatz.plugins

import com.queatz.*
import com.queatz.db.Db
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

val json = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

val secrets = try {
    json.decodeFromString<Secrets>(File("./secrets.json").reader().readText())
} catch (exception: FileNotFoundException) {
    System.err.println("The secrets.json file is missing! See Secrets.kt for the data structure.")
    throw exception
}

val db = Db()
val push = Push()
val app = App()
val notify = Notify()
val ai = Ai()
val platform = Platform()

const val defaultNearbyMaxDistanceInMeters = 100_000.0
const val defaultInventoriesNearbyMaxDistanceInMeters = 10_000.0
