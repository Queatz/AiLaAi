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

    fun button(text: String, script: String, data: String? = null) {
        result += StoryContent.Button(text, script, data)
    }
}
