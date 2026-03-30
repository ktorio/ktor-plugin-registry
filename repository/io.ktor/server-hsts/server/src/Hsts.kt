/**
 * slot://io.ktor/server-core/http
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.hsts.*
import io.ktor.server.response.*

fun Application.configureHsts() {
    install(HSTS) {
        includeSubDomains = true
    }
}
