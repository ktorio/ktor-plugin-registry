import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.github.smiley4.ktorswaggerui.SwaggerUI

// The contents of the `install` function will be used for the project template
public fun Application.install() {
    install(SwaggerUI) {
        swagger {
            swaggerUrl = "swagger-ui"
            forwardRoot = true
        }
        info {
            title = "Example API"
            version = "latest"
            description = "Example API for testing and demonstration purposes."
        }
        server {
            url = "http://localhost:8080"
            description = "Development Server"
        }
    }
}
