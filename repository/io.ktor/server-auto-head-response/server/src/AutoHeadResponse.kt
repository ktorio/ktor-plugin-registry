package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.response.*

public fun Application.configureAutoHeadResponse() {
    install(AutoHeadResponse)
}
