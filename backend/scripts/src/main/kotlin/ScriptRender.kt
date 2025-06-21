import com.queatz.db.ButtonStyle
import com.queatz.db.InputType
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
        style: ButtonStyle? = null,
        color: String? = null,
        enabled: Boolean? = null,
    ) {
        result += StoryContent.Button(
            text = text,
            script = script,
            data = data,
            style = style,
            color = color,
            enabled = enabled,
        )
    }

    fun input(
        key: String,
        value: String? = null,
        hint: String? = null,
        type: InputType = InputType.Text,
    ) {
        result += StoryContent.Input(
            key = key,
            value = value,
            hint = hint,
            inputType = type,
        )
    }

    fun photo(
        url: String,
        aspect: Float? = null,
    ) {
        result += StoryContent.Photos(listOf(url), aspect = aspect)
    }

    fun video(
        url: String,
    ) {
        result += StoryContent.Video(url)
    }

    fun audio(
        url: String,
    ) {
        result += StoryContent.Audio(url)
    }

    fun profiles(
        profiles: List<String>,
    ) {
        result += StoryContent.Profiles(profiles)
    }

    fun groups(
        groups: List<String>,
        coverPhotos: Boolean = true
    ) {
        result += StoryContent.Groups(groups, coverPhotos = coverPhotos)
    }

    fun pages(
        pages: List<String>,
    ) {
        result += StoryContent.Cards(pages)
    }

    // todo: Scene
}
