package kastle

import io.ktor.server.application.*

fun Application.configureHTTP() {
    _slots("http")
}