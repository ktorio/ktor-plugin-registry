import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.install() {
    install(Routing) {
        get {
            call.respondText("Hello, World!")
        }
    }
}
