package com.queatz.ailaai.ui.story

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.navigation.NavController
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.timeAgo
import kotlinx.datetime.Instant

@OptIn(ExperimentalTextApi::class)
@Composable
fun StoryAuthors(navController: NavController, publishDate: Instant?, authors: List<Person>) {
    val someone = stringResource(R.string.someone)
    val authorsText = buildAnnotatedString {
        append("${publishDate?.timeAgo() ?: stringResource(R.string.draft)} ${stringResource(R.string.inline_by)} ")
        authors
            .forEachIndexed { index, person ->
                if (index != 0) {
                    append(", ")
                    if (index == authors.lastIndex) {
                        append("and")
                        append(" ")
                    }
                }
                withStyle(
                    SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    withAnnotation(person.id!!, person.id!!) {
                        append(person.name ?: someone)
                    }
                }
            }
    }
    ClickableText(
        authorsText,
        style = MaterialTheme.typography.bodyMedium.merge(
            SpanStyle(
                color = MaterialTheme.colorScheme.secondary
            )
        )
    ) {
        authorsText.getStringAnnotations(it, it).firstOrNull()?.tag?.let { id ->
            navController.navigate("profile/$id")
        }
    }
}
