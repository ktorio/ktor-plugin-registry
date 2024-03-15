import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.response.*

public fun Application.configureRouting() {
    install(AutoHeadResponse)
}
