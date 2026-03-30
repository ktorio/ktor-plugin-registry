/**
 * slot://io.ktor/server-core/http
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSwagger() {
    routing {
        swaggerUI(path = "openapi")
    }
}
