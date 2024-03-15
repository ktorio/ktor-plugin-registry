import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Routing.configureRouting() {
    get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
}
