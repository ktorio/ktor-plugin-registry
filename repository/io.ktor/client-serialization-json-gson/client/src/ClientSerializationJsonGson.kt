/**
 * slot://io.ktor/client-core/clientConfig
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*

fun HttpClientConfig<*>.configureClientSerializationJsonGson() {
    install(ContentNegotiation) {
        gson()
    }
}
