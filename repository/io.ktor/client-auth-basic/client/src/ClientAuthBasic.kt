/**
 * slot://io.ktor/client-core/clientConfig
 */
package kastle

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

fun HttpClientConfig<*>.configureClientAuthBasic() {
    install(Auth) {
        basic {
            credentials {
                BasicAuthCredentials(username = "jetbrains", password = "foobar")
            }
            realm = "Access to the '/' path"
        }
    }
}
