import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.slf4j.event.*

public fun CallLoggingConfig.configureLogging() {
    level = Level.INFO
    filter { call -> call.request.path().startsWith("/") }
}
