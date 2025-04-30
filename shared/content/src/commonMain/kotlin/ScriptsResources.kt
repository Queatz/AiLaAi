package app.ailaai.shared.resources

object ScriptsResources {
    const val documentation = """
Scripts are written in Kotlin, and imports must be specified.

The following variables are available in scripts:

```kotlin
self: String // The id of this script
me: Person? // The current user, if signed in, null if signed out
data: String? // Data passed to the script, if any
secret: String? // Secret value set by the script owner, only visible to the script
```

Person has the following fields:

`id: String?`, `name: String?`, `photo: String?`, `language: String?`, `utcOffset: Double?`, `seen: Instant?`

The following are functions passed into scripts: `render`, `http`

# Rendering content

```kotlin
import com.queatz.db.ButtonStyle
import com.queatz.db.InputType

render {
    section("<Title>") // Markdown supported
    text("<Text>") // Markdown supported
    button(
        text = "<Button text>",
        script = "<Script ID to run>",
        // Optional
        data = "<Data passed to the script>",
        // Optional
        color = "<Color hex code>",
        // Optional
        style = ButtonStyle.Secondary // Default is ButtonStyle.Primary
        // Optional
        enabled = false
    )
    audio("<Url>") // Must start with /static/
    photo("<Url>", <Aspect ratio float>?) // Must start with /static/
    video("<Url>") // Must start with /static/
    input(
        key = "<key>",
        // Optional
        value = "<value>",
        // Optional
        hint = "<hint>",
        // Optional
        type = InputType.Text or InputType.Photo
    )
    profiles(
        profiles = listOf("<Profile IDs>") // List of profile IDs to display
    )
    groups(
        groups = listOf("<Group IDs>") // List of group IDs to display
        coverPhotos = false // Optional. Hide the cover photos
    )
    pages(
        pages = listOf("<Page IDs>") // List of page IDs to display as cards
    )
}
```

# Networking

Ktor's HttpClient is used for simple networking. Learn more at ktor.io.

Important: This can only be used at the root level of a script.
If you need to make HTTP calls from inside your own class, you'll need your own HTTP client.

```kotlin
http<Any type here>("<url>")
http.post<Response type here, Request type here>("<url>", body = <Any object here>)
```

KotlinX Serialization is available. The recommendation is to use `@Serializable` data classes.

```kotlin
@Serializable
data class Request(val data: String)

@Serializable
data class Response(val data: String)

val post = http.post<Response, Request>("<url>", body = Request(""))
val get = http<Response>("<url>")
val get: Response = http("<url>") // or this
val get: Response = http("<url>", headers = mapOf("Authorization" to "<token>")) // set headers
```

Other common response types are: `String`, `HttpStatusCode`, `JsonObject`

# Reading user input

When using `input("<key>", "<value>")`, the value will be passed to the script in `input`.

```kotlin
val name = (input as? Map<String, String?>)?.let { input ->
    input["<key>"]
}
```

# Storing and retrieving data

Scripts can store value in the scope of the script owner.

Basic key/value storage is available as:

```kotlin
// Store
storage["<key>"] = "<value>"

// Retrieve
storage["<key>"]
```

You can also use your own data classes. They must have a valid ArangoDB @Key field.
It's recommended to import and extend `com.queatz.scripts.store.StorageModel` which provides a correctly annotated `key` field.

```kotlin
@Serializable
data class MyModel : StorageModel() {
    var value: String? = null
}
```

And then:

```kotlin
// Store a model
storage.put(MyModel::class, MyModel()).let { model ->
    // model.key will always be populated
}

// Get by key
storage.get(MyModel::class, "<key>")?.let { model: MyModel -> }

// Get all
storage.all(MyModel::class).forEach { model: MyModel -> }

// Get all keys
storage.keys(MyModel::class).forEach { key: String -> }

// Delete by key
storage.delete(MyModel::class, "<key>")

// Query with AQL
storage.query(
    // AQL query return type (a list of these will be returned)
    MyModel::class,
    "<AQL query string>",
    // Optional binding parameters to pass into the AQL query
    mapOf("<key>" to "<value>")
).forEach { model: MyModel -> }

// Get collection name for use in AQL queries
val collection: String = storage.collection(MyModel::class)

// Check if a collection exists
storage.hasCollection(MyModel::class)
```

# Dependencies

You can depend on packages from Maven Repositories.

```kotlin
@file:Repository("<maven url>")
@file:DependsOn("<package>")
```

You can also depend on other scripts.

```kotlin
@file:DependsOnScript("<script ID>")
```

These scripts will be accessible according to their package name, or if no package name is specified, as script_<script ID> in your script.

Note that scripts have access to all parameters and variables of the script that depends on them, including the script's `secret`.

```kotlin
import script_<script ID>.*

// Use classes from script_<script ID> here

// Or without imports:
script_<script ID>.<some object>.<some field>
```

You can opt to define the package of your script:

```kotlin
package <package name>
```

Which will make them available according to your package name.

Note that in imported scripts, no variables are passed in to the script, and as such, they can't render content.

Learn more at kotlinlang.org
"""
}
