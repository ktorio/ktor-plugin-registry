import io.ktor.server.application.*
import io.ktor.server.plugins.htmx.*
import io.ktor.server.response.*

public fun Application.configureTemplating() {
    install(Htmx)
}
