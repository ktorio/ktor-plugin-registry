/**
 * slot://io.ktor/server-call-logging/callLoggingConfig
 */
package kastle

import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.slf4j.event.*

public fun CallLoggingConfig.configureCallLogging() {
    level = Level.INFO
    filter { call -> call.request.path().startsWith("/") }
}
