package components

import Styles
import androidx.compose.runtime.*
import app.softwork.routingcompose.Router
import appString
import cities.CityStyles
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun CitiesPage() {
    Style(CityStyles)

    val router = Router.current

    Div({
        style {
            property("margin", "${1.r} auto")
            maxWidth(1200.px)
            padding(0.r, 1.r, 1.r, 1.r)
            fontSize(22.px)
            lineHeight("1.5")
            minHeight(100.vh)
        }
    }) {
        var searchText by remember { mutableStateOf("") }
        SearchField(searchText, appString { search }, styles = {
            marginTop(1.r)
            marginBottom(1.r)
        }) {
            searchText = it
        }
        Div({
            classes(CityStyles.cities)
        }) {
            listOf(
                "Hồ Chí Minh, Việt Nam" to "/photos/saigon.jpg",
            ).filter {
                searchText.isBlank() || it.first.lowercase().contains(searchText.lowercase())
            }.forEach {
                Div({
                    classes(CityStyles.city)
                    style {
                        backgroundImage("url(${it.second})")
                        position(Position.Relative)
                    }
                    onClick {
                        router.navigate("/")
                    }
                }) {
                    Span({
                        classes(CityStyles.gradient)
                    }) {  }
                    Div({
                        style {
//                            borderRadius(1.r)
//                            background("rgba(0, 0, 0, .333)")
                            padding(1.r)
                            property("z-index", "1")
                        }
                    }) {
                        Div { Text(it.first) }
                        Div({
                            style {
                                fontSize(24.px)
                            }
                            classes(CityStyles.cityAbout)
                        }) {
                            Text("Một nơi của mọi thứ, mọi người và tất cả những gì đã và đang tồn tại. Một khát vọng của thế giới nói chung.")
                        }
                        Div({
                            style {
                                fontSize(16.px)
                                opacity(.75f)
                                marginTop(.5.r)
                                color(Styles.colors.tertiary)
                            }
                        }) {
                            Text("2 người đang ở đay")
                        }
                    }
                }
            }
        }
    }
}
