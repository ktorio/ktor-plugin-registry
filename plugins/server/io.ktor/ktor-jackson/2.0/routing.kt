import com.fasterxml.jackson.databind.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Routing.configureRouting() {
    get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }
}
