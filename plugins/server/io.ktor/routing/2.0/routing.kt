import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Routing.configureRouting() {
    get("/") {
        call.respondText("Hello World!")
    }
}
