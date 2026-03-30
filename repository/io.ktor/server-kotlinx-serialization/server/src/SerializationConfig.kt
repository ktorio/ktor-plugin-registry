/**
 * slot://io.ktor/server-content-negotiation/serializationConfig
 */
package kastle

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*

fun ContentNegotiationConfig.configureKotlinxSerialization() {
    json()
}
