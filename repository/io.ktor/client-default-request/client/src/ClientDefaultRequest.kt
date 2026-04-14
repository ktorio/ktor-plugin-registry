/**
 * slot://io.ktor/client-core/clientConfig
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.*

fun HttpClientConfig<*>.configureClientDefaultRequest() {
    defaultRequest {
        url("https://ktor.io/docs/")
    }
}
