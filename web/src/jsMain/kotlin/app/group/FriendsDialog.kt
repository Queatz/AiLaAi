package app.group

import androidx.compose.runtime.*
import api
import app.AppStyles
import app.ailaai.api.groups
import app.dialog.dialog
import application
import com.queatz.db.Person
import components.Loading
import components.ProfilePhoto
import focusable
import lib.formatDistanceToNow
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

suspend fun friendsDialog(omit: List<String> = emptyList(), onPerson: (Person) -> Unit) = dialog(
    application.appString { invite },
    cancelButton = null,
    confirmButton = application.appString { close }
) { resolve ->
    var isLoading by remember {
        mutableStateOf(true)
    }
    var people by remember {
        mutableStateOf(emptyList<Person>())
    }

    LaunchedEffect(Unit) {
        api.groups {
            people = it.mapNotNull { it.members }
                .flatMap { it.mapNotNull { it.person } }
                .filter { it.id !in omit }
                .distinctBy { it.id }
                .sortedByDescending { it.seen?.toEpochMilliseconds() ?: 0 }
                .toList()
        }
        isLoading = false
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        if (isLoading) {
            Loading {
                style {
                    padding(2.r)
                }
            }
            return@Div
        }
        people.forEach { person ->
            Div({
                classes(
                    listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                )
                onClick {
                    onPerson(person)
                    resolve(true)
                }

                focusable()
            }) {
                ProfilePhoto(person)
                Div({
                    style {
                        marginLeft(1.r)
                    }
                }) {
                    Div({
                        classes(AppStyles.groupItemName)
                    }) {
                        Text(person.name ?: application.appString { someone })
                    }
                    Div({
                        classes(AppStyles.groupItemMessage)
                    }) {
                        Text(
                            "${application.appString { active }} ${
                                formatDistanceToNow(
                                    Date(person.seen?.toEpochMilliseconds() ?: person.createdAt!!.toEpochMilliseconds()),
                                    js("{ addSuffix: true }")
                                )
                            }"
                        )
                    }
                }
            }
        }
    }
}
