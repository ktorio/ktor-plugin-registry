/**
 * slot://io.ktor/client-core/http
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.plugins.resources.Resources
import io.ktor.resources.*

fun HttpClientConfig<*>.configureClientResources() {
    install(Resources)
}
