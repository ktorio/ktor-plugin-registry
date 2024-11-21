import io.ktor.server.application.*
import io.ktor.server.routing.*

public fun Application.module() {
    routing {
        configureRouting()
    }
}
