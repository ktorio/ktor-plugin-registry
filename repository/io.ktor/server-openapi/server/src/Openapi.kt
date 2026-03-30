/**
 * slot://io.ktor/server-core/http
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureOpenapi() {
    routing {
        openAPI(path = "openapi")
    }
}
