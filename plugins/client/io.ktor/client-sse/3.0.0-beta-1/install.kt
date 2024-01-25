import io.ktor.client.*
import io.ktor.client.plugins.sse.*

fun HttpClientConfig<*>.install() {
    install(SSE)
}
