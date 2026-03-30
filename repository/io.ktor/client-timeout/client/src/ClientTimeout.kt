/**
 * slot://io.ktor/client-core/http
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.*

fun HttpClientConfig<*>.configureClientTimeout() {
    install(HttpTimeout) {
        requestTimeoutMillis = 1000
    }
}
