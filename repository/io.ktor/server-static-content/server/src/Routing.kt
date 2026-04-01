/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Routing.configureStaticContent() {
    // Static plugin. Try to access `/static/index.html`
    staticResources("/static", "static")
}
