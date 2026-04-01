/**
 * slot://io.ktor/server-core/routing
 */
package kastle

import io.github.cotrin8672.LineWebhook
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.configureLineWebhook() {
    route("/callback") {
        install(DoubleReceive)
        install(LineWebhook) {
            channelSecret = application.propertyOrNull("line.webhook.secret") ?: "s3¢r3t"
        }
        post {
            call.respond(HttpStatusCode.OK)
        }
    }
}
