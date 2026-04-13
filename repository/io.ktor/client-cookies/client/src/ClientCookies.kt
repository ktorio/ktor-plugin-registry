package kastle

import io.ktor.client.*
import io.ktor.client.plugins.cookies.*

fun HttpClientConfig<*>.configureClientCookies() {
    install(HttpCookies)
}
