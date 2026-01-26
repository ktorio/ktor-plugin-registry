/**
 * slot://io.ktor/server-core/monitoring
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.micrometer.prometheus.*

public fun Application.configureMetricsMicrometer() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
        // ...
    }
}
