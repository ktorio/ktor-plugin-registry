package kastle

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        if (_slots.contains("routingRoot")) {
            _slot("routingRoot")
        } else {
            get("/") {
                call.respondText("Hello, World!")
            }
        }
        _slots("routing")
    }
}