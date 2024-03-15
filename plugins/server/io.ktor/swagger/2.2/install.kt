import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Application.configureHTTP() {
    routing {
        swaggerUI(path = "openapi")
    }
}
