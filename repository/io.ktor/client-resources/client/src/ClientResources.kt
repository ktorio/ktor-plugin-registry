package kastle

import io.ktor.client.*
import io.ktor.client.plugins.resources.*

fun HttpClientConfig<*>.configureClientResources() {
    install(Resources)
}
