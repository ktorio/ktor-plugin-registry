/**
 * slot://io.ktor/server-core/monitoring
 */
package kastle

import io.ktor.server.application.*
import dev.hayden.KHealth

// The contents of the `install` function will be used for the project template
fun Application.configureKhealth() {
    install(KHealth)
}
