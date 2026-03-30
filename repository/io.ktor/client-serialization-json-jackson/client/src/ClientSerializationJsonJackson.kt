/**
 * slot://io.ktor/client-core/serialization
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*

fun HttpClientConfig<*>.configureClientSerializationJsonJackson() {
    install(ContentNegotiation) {
        jackson()
    }
}
