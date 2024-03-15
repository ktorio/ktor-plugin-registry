import io.ktor.client.*
import io.ktor.client.plugins.cookies.*

public fun HttpClientConfig<*>.configure() {
    install(HttpCookies)
}
