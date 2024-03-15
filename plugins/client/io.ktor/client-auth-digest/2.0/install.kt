import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

public fun HttpClientConfig<*>.configure() {
    install(Auth) {
        digest {
            credentials {
                DigestAuthCredentials(username = "jetbrains", password = "foobar")
            }
            realm = "Access to the '/' path"
        }
    }
}
