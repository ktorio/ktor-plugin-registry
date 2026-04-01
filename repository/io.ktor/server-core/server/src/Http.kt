package kastle

import io.ktor.server.application.*

fun Application.configureHttp() {
    _slots("http")
}
