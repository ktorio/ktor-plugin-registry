import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Routing.configureRouting() {
    // Static plugin. Try to access `/static/index.html`
    staticResources("/static", "static")
}
