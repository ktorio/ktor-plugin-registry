import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.xml.*

public fun HttpClientConfig<*>.configure() {
    install(ContentNegotiation) {
        xml()
    }
}
