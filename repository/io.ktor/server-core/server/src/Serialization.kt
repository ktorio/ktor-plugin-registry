package kastle

import io.ktor.server.application.*

fun Application.configureSerialization() {
    _slots("serialization")
}