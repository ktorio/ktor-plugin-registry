import com.ucasoft.ktor.simpleCache.cacheOutput
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

public fun Route.cachedEndpoints() {
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
