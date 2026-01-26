/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Routing.configureKotlinxSerialization() {
    get("/json/kotlinx-serialization") {
        call.respond(mapOf("hello" to "world"))
    }
}
