/**
 * slot://io.ktor/server-core/security
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.sessions.*

public fun Application.configureSessions() {
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }
}
