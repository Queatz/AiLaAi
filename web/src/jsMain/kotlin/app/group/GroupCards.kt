import androidx.compose.runtime.*
import app.AppStyles
import app.ailaai.api.groupCards
import app.ailaai.api.newCard
import app.cards.CardsPageStyles
import app.components.TopBarSearch
import app.dialog.inputDialog
import com.queatz.db.Card
import com.queatz.db.GroupExtended
import components.CardItem
import components.Loading
import components.Tip
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div

@Composable
fun GroupCards(group: GroupExtended) {
    Style(CardsPageStyles)

    val scope = rememberCoroutineScope()

    var isLoading by remember {
        mutableStateOf(true)
    }

    var cards by remember(group) {
        mutableStateOf(listOf<Card>())
    }

    var search by remember {
        mutableStateOf("")
    }

    suspend fun reload() {
        if (group.group?.id == null) return
        api.groupCards(group.group!!.id!!) {
            cards = it
        }
        isLoading = false
    }

    LaunchedEffect(group) {
        reload()
    }

    val shownCards by remember(cards, search) {
        mutableStateOf(
            if (search.isBlank()) {
                cards
            } else {
                cards.filter {
                    it.name?.contains(search, ignoreCase = true) == true
                }
            }
        )
    }

    fun newCard() {
        scope.launch {
            val result = inputDialog(
                application.appString { createCard },
                application.appString { title },
                application.appString { create }
            )

            if (result == null) return@launch


            api.newCard(
                Card(
                    group = group.group!!.id!!,
                    name = result
                )
            ) {
                reload()
            }
        }
    }

    if (isLoading) {
        Loading()
    } else {
        Div({
            classes(AppStyles.groupCards)
        }) {
            if (cards.isEmpty()) {
                Tip(
                    // todo: translate
                    text = "This group currently has no pages.",
                    action = appString { createCard },
                    styles = {
                        margin(1.r)
                    }
                ) {
                    newCard()
                }
            } else {
                if (cards.size > 0) {
                    TopBarSearch(search, { search = it }) {
                        marginTop(1.r)
                        marginBottom(1.r)
                        marginLeft(1.r)
                        marginRight(1.r)
                    }
                }

                Div(
                    {
                        classes(CardsPageStyles.layout)
                        style {
                            paddingBottom(1.r)
                        }
                    }
                ) {
                    shownCards.forEach { card ->
                        CardItem(card, openInNewWindow = true)
                    }
                }
            }
        }
    }
}
