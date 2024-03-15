import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*

public fun HttpClientConfig<*>.configure() {
    install(ContentNegotiation) {
        jackson()
    }
}
