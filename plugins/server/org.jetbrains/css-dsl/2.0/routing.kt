import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.css.*
import kotlinx.html.*

public fun Routing.configureRouting() {
    get("/styles.css") {
        call.respondCss {
            body {
                backgroundColor = Color.darkBlue
                margin(0.px)
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
