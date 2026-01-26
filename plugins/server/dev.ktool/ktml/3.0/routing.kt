import dev.ktml.ktor.respondKtml
import io.ktor.server.application.Application
import io.ktor.server.routing.*

fun Routing.configureRouting() {
    get("ktml") {
        call.respondKtml(
            path = "index",
            model = mapOf("name" to "KTML")
        )
    }
}