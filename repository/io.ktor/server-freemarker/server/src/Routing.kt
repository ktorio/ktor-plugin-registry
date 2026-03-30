/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import freemarker.cache.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.configureFreemarker() {
    get("/html-freemarker") {
        call.respond(FreeMarkerContent("index.ftl", mapOf("data" to IndexData(listOf(1, 2, 3))), ""))
    }
}
