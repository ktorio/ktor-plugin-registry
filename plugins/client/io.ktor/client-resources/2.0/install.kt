import io.ktor.client.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.plugins.resources.Resources
import io.ktor.resources.*

public fun HttpClientConfig<*>.configure() {
    install(Resources)
}
