import com.queatz.db.ButtonStyle
import com.queatz.db.StoryContent

class ScriptRender(private val onRender: (List<StoryContent>) -> Unit) {
    private var result = mutableListOf<StoryContent>()

    operator fun invoke(block: ScriptRender.() -> Unit) {
        block()
        onRender(result)
    }

    fun section(section: String) {
        result += StoryContent.Section(section)
    }

    fun text(text: String) {
        result += StoryContent.Text(text)
    }

    fun button(
        text: String,
        script: String,
        data: String? = null,
        style: ButtonStyle? = null
    ) {
        result += StoryContent.Button(text = text, script = script, data = data, style = style)
    }

    fun input(
        key: String,
        value: String? = null,
    ) {
        result += StoryContent.Input(key = key, value = value)
    }

    fun photo(
        url: String,
        aspect: Float? = null,
    ) {
        result += StoryContent.Photos(listOf(url), aspect = aspect)
    }
}
