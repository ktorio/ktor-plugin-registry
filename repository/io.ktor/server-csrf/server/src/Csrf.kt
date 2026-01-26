/**
 * slot://io.ktor/server-core/security
 */
package kastle


import io.ktor.server.application.*
import io.ktor.server.plugins.csrf.*

public fun Application.configureCsrf() {
    install(CSRF) {
        // tests Origin is an expected value
        allowOrigin("http://localhost:8080")

        // tests Origin matches Host header
        originMatchesHost()

        // custom header checks
        checkHeader("X-CSRF-Token")
    }
}
