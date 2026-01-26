/**
 * slot://io.ktor/server-core/http
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.response.*

public fun Application.configureCompression() {
    install(Compression)
}
