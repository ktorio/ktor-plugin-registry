import io.ktor.client.*
import io.ktor.client.plugins.*

public fun HttpClientConfig<*>.configure() {
    defaultRequest {
        url("https://ktor.io/docs/")
    }
}
