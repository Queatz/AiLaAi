import kotlinx.serialization.Serializable

@Serializable
data class PageTreeData(
    var card: String? = null,
    var votes: Map<String, Int> = emptyMap()
)
