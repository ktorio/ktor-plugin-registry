package kastle

import io.ktor.server.application.*

fun Application.configureMonitoring() {
    _slots("monitoring")
}