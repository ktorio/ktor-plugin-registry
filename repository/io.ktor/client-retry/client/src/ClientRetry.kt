package kastle

import io.ktor.client.*
import io.ktor.client.plugins.*

fun HttpClientConfig<*>.configureClientRetry() {
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 5)
        exponentialDelay()
    }
}
