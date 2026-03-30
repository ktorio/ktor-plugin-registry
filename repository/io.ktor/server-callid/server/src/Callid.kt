/**
 * slot://io.ktor/server-core/monitoring
 */
package kastle

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.response.*

fun Application.configureCallid() {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
}
