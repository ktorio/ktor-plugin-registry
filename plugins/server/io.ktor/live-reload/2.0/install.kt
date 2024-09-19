import io.ktor.server.application.*
import io.ktor.server.plugins.live.reload.*

fun Application.configure() {
    install(LiveReload)
}