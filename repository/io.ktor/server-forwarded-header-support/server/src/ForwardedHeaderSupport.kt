/**
 * slot://io.ktor/server-core/http
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.response.*

public fun Application.configureForwardedHeaderSupport() {
    install(ForwardedHeaders) // WARNING: for security, do not include this if not behind a reverse proxy
    install(XForwardedHeaders) // WARNING: for security, do not include this if not behind a reverse proxy
}
