/**
 * slot://io.ktor/client-core/http
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.compression.*

fun HttpClientConfig<*>.configureClientContentEncoding() {
    install(ContentEncoding) {
        deflate(1.0F)
        gzip(0.9F)
    }
}
