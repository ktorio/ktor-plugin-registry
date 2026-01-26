/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.velocity.*
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader

public fun Routing.configureVelocity() {
    get("/index") {
        val sampleUser = VelocityUser(1, "John")
        call.respond(VelocityContent("templates/index.vl", mapOf("user" to sampleUser)))
    }
}
