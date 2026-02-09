/**
 * slot://io.ktor/client-core/http
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.cookies.*

public fun HttpClientConfig<*>.configureClientCookies() {
    install(HttpCookies)
}
