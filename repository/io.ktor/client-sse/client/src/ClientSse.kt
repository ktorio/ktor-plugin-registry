/**
 * slot://io.ktor/client-core/clientConfig
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.sse.*

fun HttpClientConfig<*>.configureClientSse() {
    install(SSE)
}
