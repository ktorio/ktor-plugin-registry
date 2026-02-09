/**
 * slot://io.ktor/client-core/serialization
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*

public fun HttpClientConfig<*>.configureClientSerializationJsonGson() {
    install(ContentNegotiation) {
        gson()
    }
}
