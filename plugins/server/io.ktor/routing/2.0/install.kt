import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Application.configureRouting() {
    routing {
        configureRouting()
    }
}
