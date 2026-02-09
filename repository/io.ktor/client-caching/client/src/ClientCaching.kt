/**
 * slot://io.ktor/client-core/http
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.cache.*

public fun HttpClientConfig<*>.configureClientCaching() {
    install(HttpCache)
}
