/**
 * slot://io.ktor/server-content-negotiation/serializationConfig
 */
package kastle

import io.ktor.serialization.gson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*

public fun ContentNegotiationConfig.configureGson() {
    gson {
        }
}
