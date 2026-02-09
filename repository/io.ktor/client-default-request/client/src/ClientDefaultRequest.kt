/**
 * slot://io.ktor/client-core/http
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.*

public fun HttpClientConfig<*>.configureClientDefaultRequest() {
    defaultRequest {
        url("https://ktor.io/docs/")
    }
}
