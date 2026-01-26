import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.openapi.*

public fun Application.configureHTTP() {
    routing {
        swaggerUI(path = "openapi") {
            info = OpenApiInfo(title = "My API", version = "1.0.0")
        }
    }
}
