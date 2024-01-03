import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest

val indicator = Indicator()

enum class IndicatorSource {
    Group
}

@OptIn(ExperimentalCoroutinesApi::class)
class Indicator {

    private val indicators = MutableStateFlow(emptySet<IndicatorSource>())

    val hasIndicator = indicators.mapLatest { it.isNotEmpty() }

    fun set(source: IndicatorSource, count: Int) {
        if (count > 0) {
            indicators.value += source
        } else {
            indicators.value -= source
        }
    }
}
