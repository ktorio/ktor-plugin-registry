import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Routing.configureRouting() {
    get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
}
