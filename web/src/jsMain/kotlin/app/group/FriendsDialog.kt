package app.group

import androidx.compose.runtime.*
import api
import app.AppStyles
import app.ailaai.api.groups
import app.components.Empty
import app.dialog.dialog
import app.messaages.inList
import appString
import application
import com.queatz.db.Person
import components.Loading
import components.ProfilePhoto
import components.SearchField
import focusable
import lib.formatDistanceToNow
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

suspend fun friendsDialog(
    title: String = application.appString { invite },
    items: List<Person>? = null,
    omit: List<String> = emptyList(),
    preselect: Set<String> = emptySet(),
    multiple: Boolean = false,
    confirmButton: String = application.appString { close },
    actions: @Composable (resolve: (Boolean?) -> Unit) -> Unit = {},
    content: @Composable (resolve: (Boolean?) -> Unit) -> Unit = {},
    onPeople: (Set<Person>) -> Unit
) {
    var selectedPeople = emptySet<Person>()

    val result = dialog(
        title = title,
        cancelButton = null,
        confirmButton = confirmButton,
        actions = actions,
        maxWidth = 800.px
    ) { resolve ->
        var people by remember { mutableStateOf(items ?: emptyList()) }
        var search by remember { mutableStateOf("") }
        var selected by remember { mutableStateOf(preselect) }
        var isLoading by remember {
            mutableStateOf(people.isEmpty())
        }

        val shownPeople = remember(people, search) {
            if (search.isBlank()) {
                people
            } else {
                people.filter {
                    it.name?.contains(search, ignoreCase = true) == true
                }
            }.filter { it.id !in omit }
        }

        if (items == null) {
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
        }

        LaunchedEffect(selected) {
            selectedPeople = people.filter { it.id in selected }.toSet()
        }

        content(resolve)

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

            if (people.size > 5) {
                SearchField(search, placeholder = appString { this.search }, shadow = false, styles = {
                    marginBottom(1.r)
                }) { search = it }
            }

            if (shownPeople.isEmpty()) {
                Empty {
                    // todo: translate
                    Text("No people.")
                }
            } else {
                shownPeople.forEachIndexed { index, person ->
                    Div({
                        classes(
                            listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                        )

                        if (person.id in selected) {
                            classes(AppStyles.groupItemSelectedPrimary)

                            ((index > 0 && shownPeople[index - 1].id in selected) to (index < shownPeople.lastIndex && shownPeople[index + 1].id in selected)).let { (aboveIsSelected, belowIsSelected) ->
                                when {
                                    aboveIsSelected && belowIsSelected -> {
                                        style {
                                            borderRadius(0.r)
                                        }
                                    }

                                    aboveIsSelected -> {
                                        style {
                                            borderRadius(0.r, 0.r, 1.r, 1.r)
                                        }
                                    }

                                    belowIsSelected -> {
                                        style {
                                            borderRadius(1.r, 1.r, 0.r, 0.r)
                                        }
                                    }
                                }
                            }
                        }

                        onClick {
                            if (multiple) {
                                selected = if (person.id in selected) {
                                    selected - person.id!!
                                } else {
                                    selected + person.id!!
                                }

                            } else {
                                onPeople(setOf(person))
                                resolve(false)
                            }
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
    }

    if (result == true) {
        onPeople(selectedPeople)
    }
}
