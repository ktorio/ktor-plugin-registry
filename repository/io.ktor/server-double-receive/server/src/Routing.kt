/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.configureDoubleReceive() {
    post("/double-receive") {
        val first = call.receiveText()
        val theSame = call.receiveText()
        call.respondText(first + " " + theSame)
    }
}
