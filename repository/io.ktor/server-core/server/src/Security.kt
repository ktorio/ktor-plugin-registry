package kastle

import io.ktor.server.application.*

fun Application.configureSecurity() {
    _slots("security")
}