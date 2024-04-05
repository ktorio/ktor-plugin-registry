import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Routing.cachedEndpoints() {
    routing {
        cacheOutput(2.seconds) {
            get("/short") {
                call.respond(Random.nextInt())
            }
        }
        cacheOutput {
            get("/default") {
                call.respond(Random.nextInt())
            }
        }
    }
}
