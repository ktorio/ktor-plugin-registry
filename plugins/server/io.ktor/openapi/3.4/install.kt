import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.routing.*

public fun Application.configureHTTP() {
    routing {
        openAPI(path = "openapi") {
            info = OpenApiInfo(title = "My API", version = "1.0.0")
        }
    }
}
