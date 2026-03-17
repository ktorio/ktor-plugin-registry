/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.github.cotrin8672.LineWebhook

public fun Route.configureLineWebhook() {
    route("/callback") {
        install(DoubleReceive)
        install(LineWebhook) {
            channelSecret = System.getenv("CHANNEL_SECRET")
        }
        post {
            call.respond(HttpStatusCode.OK)
        }
    }
}
