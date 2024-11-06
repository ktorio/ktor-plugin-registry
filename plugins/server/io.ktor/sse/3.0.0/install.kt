import io.ktor.server.application.*
import io.ktor.server.sse.*

fun Application.install() {
    install(SSE)
}
