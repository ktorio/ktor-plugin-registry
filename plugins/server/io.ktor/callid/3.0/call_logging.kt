import io.ktor.http.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*

public fun CallLoggingConfig.configureLogging() {
    callIdMdc("call-id")
}
