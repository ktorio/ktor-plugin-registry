import io.ktor.client.*
import io.ktor.client.plugins.*

public fun HttpClientConfig<*>.configure() {
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 5)
        exponentialDelay()
    }
}
