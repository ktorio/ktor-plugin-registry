/**
 * slot://io.ktor/server-core/monitoring
 */
package kastle

import com.codahale.metrics.*
import io.ktor.server.application.*
import io.ktor.server.metrics.dropwizard.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

fun Application.configureMetrics() {
    install(DropwizardMetrics) {
        Slf4jReporter.forRegistry(registry)
            .outputTo(LoggerFactory.getLogger("metrics"))
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()
            .start(10, TimeUnit.SECONDS)
    }
}
