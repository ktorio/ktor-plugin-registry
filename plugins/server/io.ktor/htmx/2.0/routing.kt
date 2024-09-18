import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import java.io.File

fun Routing.configureRouting() {
    val scriptPattern = Regex(".+?\\.js(\\.map)?")

    get("/") {
        call.respondHtml {
            head {
                title("HTMX Demo")
                script { src = "main.js" }
            }
            body {
                button {
                    attributes["hx-get"] = "/htmx"
                    attributes["hx-swap"] = "outerHtml"

                    +"Test"
                }
            }
        }
    }

    get(scriptPattern) {
        val requestPath = call.request.path()
        val resolvedFile = File("build/dist/js/productionExecutable/$requestPath")
        if (resolvedFile.exists())
            call.respondFile(resolvedFile)
        else call.respond(HttpStatusCode.NotFound)
    }

    get("/htmx") {
        call.respondHtml {
            body {
                div {
                    +"Hello, HTMX"
                }
            }
        }
    }
}
