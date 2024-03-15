import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.*

public fun Routing.configureRouting(appMicrometerRegistry: PrometheusMeterRegistry) {
    get("/metrics-micrometer") {
        call.respond(appMicrometerRegistry.scrape())
    }
}
