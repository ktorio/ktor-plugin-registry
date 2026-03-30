/**
 * slot://io.ktor/server-core/http
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.response.*

fun Application.configureHttpsRedirect() {
    install(HttpsRedirect) {
            // The port to redirect to. By default 443, the default HTTPS port.
            sslPort = 443
            // 301 Moved Permanently, or 302 Found redirect.
            permanentRedirect = true
        }
}
