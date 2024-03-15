import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

public fun HttpClientConfig<*>.configure() {
    install(Auth) {
        basic {
            credentials {
                BasicAuthCredentials(username = "jetbrains", password = "foobar")
            }
            realm = "Access to the '/' path"
        }
    }
}
