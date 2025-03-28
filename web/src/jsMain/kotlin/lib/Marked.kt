package lib

@JsModule("marked")
@JsNonModule
external object marked {
    class Renderer {
//        var heading: (text: String, level: Int, raw: String?) -> String
//        var paragraph: (text: String) -> String
//        var code: (code: String, language: String?) -> String
//        var blockquote: (quote: String) -> String
//        var list: (body: String, ordered: Boolean) -> String
//        var listitem: (text: String) -> String
//        var strong: (text: String) -> String
//        var em: (text: String) -> String
//        var codespan: (code: String) -> String
        var link: (data: RendererLinkToken) -> String = definedExternally
//        var image: (href: String, title: String?, text: String) -> String
//        var table: (header: String, body: String) -> String
    }

    fun parse(text: String, options: dynamic = definedExternally): String

    interface RendererLinkToken {
        val href: String
        val raw: String
        val title: String?
        val text: String?
    }
}
