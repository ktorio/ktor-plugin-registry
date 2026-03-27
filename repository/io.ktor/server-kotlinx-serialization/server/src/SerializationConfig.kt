/**
 * slot://io.ktor/server-content-negotiation/serializationConfig
 */
package kastle

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*

public fun ContentNegotiationConfig.configureKotlinxSerialization() {
    json()
}
