import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Application.configureHTTP() {
    routing {
        openAPI(path = "openapi")
    }
}
