package lib

import org.khronos.webgl.Uint8Array

@JsModule("@paulmillr/qr")
@JsNonModule
external object Qr {
    @JsName("default")
    fun createQR(text: String, output: String = definedExternally, opts: dynamic = definedExternally): Uint8Array
}
