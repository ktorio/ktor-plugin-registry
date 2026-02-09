/**
 * slot://io.ktor/client-core/http
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.*

public fun HttpClientConfig<*>.configureClientUserAgent() {
    install(UserAgent) {
        agent = "Ktor client"
    }
}
