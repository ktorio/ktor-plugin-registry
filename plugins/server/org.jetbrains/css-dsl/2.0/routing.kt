import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.css.*
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.link

public fun Routing.configureRouting() {
    get("/styles.css") {
        call.respondCss {
            body {
                backgroundColor = Color.darkBlue
                margin = Margin(0.px)
            }
            rule("h1.page-title") {
                color = Color.white
            }
        }
    }
    
    get("/html-css-dsl") {
        call.respondHtml {
            head {
                link(rel = "stylesheet", href = "/styles.css", type = "text/css")
            }
            body {
                h1(classes = "page-title") {
                    +"Hello from Ktor!"
                }
            }
        }
    }
}
