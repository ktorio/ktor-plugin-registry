package kastle

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.webjars.*

public fun Application.configureWebjars() {
    install(Webjars) {
        path = "/webjars" //defaults to /webjars
    }
}
