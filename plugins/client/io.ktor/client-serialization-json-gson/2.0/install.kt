import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*

public fun HttpClientConfig<*>.configure() {
    install(ContentNegotiation) {
        gson()
    }
}
