/**
 * slot://io.ktor/server-core/monitoring
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.micrometer.prometheus.*

public fun Application.configureMetricsMicrometer() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
        // ...
    }

    routing {
        get("/metrics-micrometer") {
            call.respond(appMicrometerRegistry.scrape())
        }
    }
}
