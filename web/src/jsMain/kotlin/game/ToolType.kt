package game

/**
 * Enum representing the different tool types available in the editor.
 */
enum class ToolType {
    DRAW,
    CLONE,
    SKETCH,
    BUCKET,
    LINE;

    companion object {
        /**
         * Convert from string representation to enum value
         */
        fun fromString(value: String?): ToolType? {
            return when (value?.lowercase()) {
                "draw" -> DRAW
                "clone" -> CLONE
                "sketch" -> SKETCH
                "bucket" -> BUCKET
                "line" -> LINE
                else -> null
            }
        }

        /**
         * Convert from enum value to string representation
         */
        fun toString(value: ToolType?): String? {
            return when (value) {
                DRAW -> "draw"
                CLONE -> "clone"
                SKETCH -> "sketch"
                BUCKET -> "bucket"
                LINE -> "line"
                null -> null
            }
        }
    }
}
