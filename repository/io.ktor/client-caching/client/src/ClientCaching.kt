/**
 * slot://io.ktor/client-core/clientConfig
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.cache.*

fun HttpClientConfig<*>.configureClientCaching() {
    install(HttpCache)
}
