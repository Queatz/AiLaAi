import org.w3c.dom.HTMLTextAreaElement

fun HTMLTextAreaElement.resize() {
    val marginBottom = style.marginBottom
    style.marginBottom = "${scrollHeight + 2}px"
    style.height = "0"
    style.height = "${scrollHeight + 2}px"
    style.marginBottom = marginBottom
}
