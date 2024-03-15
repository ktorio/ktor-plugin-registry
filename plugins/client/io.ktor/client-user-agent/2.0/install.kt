import io.ktor.client.*
import io.ktor.client.plugins.*

public fun HttpClientConfig<*>.configure() {
    install(UserAgent) {
        agent = "Ktor client"
    }
}
