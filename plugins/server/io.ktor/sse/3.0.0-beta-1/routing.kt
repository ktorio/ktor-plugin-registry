import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*

fun Routing.install() {
    sse("/hello") {
        send(ServerSentEvent("world"))
    }
}
