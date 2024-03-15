import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

public fun HttpClientConfig<*>.configure() {
    install(Auth) {
        bearer {
            loadTokens {
                BearerTokens("abc123", "xyz111")
            }
        }
    }
}
