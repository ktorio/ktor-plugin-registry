/**
 * slot://io.ktor/client-core/monitoring
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.logging.*

public fun HttpClientConfig<*>.configureClientCallLogging() {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.HEADERS
    }
}
