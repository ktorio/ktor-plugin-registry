/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*

fun Routing.configureSse() {
    sse("/hello") {
        send(ServerSentEvent("world"))
    }
}
