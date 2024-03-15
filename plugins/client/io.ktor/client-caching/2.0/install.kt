import io.ktor.client.*
import io.ktor.client.plugins.cache.*

public fun HttpClientConfig<*>.configure() {
    install(HttpCache)
}
