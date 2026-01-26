package kastle

import io.ktor.server.application.*

fun Application.configureDatabases() {
    _slots("databases")
}