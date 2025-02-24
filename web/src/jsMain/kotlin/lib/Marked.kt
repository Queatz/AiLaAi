package lib

@JsModule("marked")
@JsNonModule
external object marked {
    fun parse(text: String): String
}
