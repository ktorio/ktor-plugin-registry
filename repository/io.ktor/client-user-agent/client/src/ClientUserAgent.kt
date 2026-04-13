package kastle

import io.ktor.client.*
import io.ktor.client.plugins.*

fun HttpClientConfig<*>.configureClientUserAgent() {
    install(UserAgent) {
        agent = "Ktor client"
    }
}
