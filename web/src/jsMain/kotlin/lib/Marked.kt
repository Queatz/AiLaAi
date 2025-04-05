package lib

@JsModule("marked")
@JsNonModule
external object marked {
    class Renderer {
        var link: (data: RendererLinkToken) -> String = definedExternally
        var html: (data: RendererHtmlToken) -> String = definedExternally
    }

    fun parse(text: String, options: dynamic = definedExternally): String

    interface RendererLinkToken {
        var href: String
        var raw: String
        var title: String?
        var text: String?
    }

    interface RendererHtmlToken {
        var raw: String
        var text: String
    }
}
