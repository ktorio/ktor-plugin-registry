/**
 * slot://io.ktor/client-core/serialization
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

public fun HttpClientConfig<*>.configureClientSerializationJsonKotlinx() {
    install(ContentNegotiation) {
        json()
    }
}
