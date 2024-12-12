import org.jetbrains.compose.web.css.StyleScope



fun StyleScope.shadow(elevation: Int = 1) {
    property("box-shadow", "${elevation}px ${elevation}px ${elevation * 4}px rgba(0, 0, 0, 0.125)")
}
