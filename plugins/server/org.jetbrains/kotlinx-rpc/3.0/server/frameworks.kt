import io.ktor.server.application.*
import io.ktor.server.routing.*

// Placeholder for test, so that our install matches the expected generated function
fun Application.configureFrameworks() {
    configureRPC()
    routing {
        configureRouting()
    }
}