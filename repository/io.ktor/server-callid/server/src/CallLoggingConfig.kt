/**
 * slot://io.ktor/server-call-logging/callLoggingConfig
 */
package kastle

import io.ktor.http.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*

fun CallLoggingConfig.configureCallid() {
    callIdMdc("call-id")
}
