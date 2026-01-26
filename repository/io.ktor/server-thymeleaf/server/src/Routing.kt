/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.Thymeleaf
import io.ktor.server.thymeleaf.ThymeleafContent
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

public fun Routing.configureThymeleaf() {
    get("/html-thymeleaf") {
        call.respond(ThymeleafContent("index", mapOf("user" to ThymeleafUser(1, "user1"))))
    }
}
