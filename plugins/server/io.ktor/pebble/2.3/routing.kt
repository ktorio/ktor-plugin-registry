import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pebbletemplates.pebble.loader.ClasspathLoader

public fun Routing.configureRouting() {
    get("/pebble-index") {
        val sampleUser = PebbleUser(1, "John")
        call.respond(PebbleContent("pebble-index.html", mapOf("user" to sampleUser)))
    }
}
