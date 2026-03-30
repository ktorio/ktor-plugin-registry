/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Routing.configureResources() {
    get<Articles> { article ->
        // Get all articles ...
        call.respond("List of articles sorted starting from ${article.sort}")
    }
}
