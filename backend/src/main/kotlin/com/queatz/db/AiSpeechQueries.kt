package com.queatz.db

fun Db.aiSpeechByText(
    text: String
) = one(
    AiSpeech::class,
    """
        for x in @@collection
            filter x.${f(AiSpeech::text)} == @text
            limit 1
            return x
    """,
    mapOf(
        "text" to text,
    )
)
