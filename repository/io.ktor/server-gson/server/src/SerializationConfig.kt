/**
 * slot://io.ktor/server-content-negotiation/serializationConfig
 */
package kastle

import io.ktor.serialization.gson.*
import io.ktor.server.plugins.contentnegotiation.*

fun ContentNegotiationConfig.configureGson() {
    gson {}
}
