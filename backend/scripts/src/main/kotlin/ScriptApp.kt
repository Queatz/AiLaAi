
interface ScriptApp {

    suspend fun download(
        url: String,
        name: String = url.substringAfterLast('/'),
    ): String

}
