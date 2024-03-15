import io.ktor.client.*
import io.ktor.client.plugins.logging.*

public fun HttpClientConfig<*>.configure() {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.HEADERS
    }
}
