/**
 * slot://io.ktor/server-core/serialization
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        _slot("serializationConfig")
    }
}
