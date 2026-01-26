/**
 * slot://io.ktor/server-core/security
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

public fun Application.configureAuthBasic() {
    authentication {
        basic(name = "myauth1") {
            realm = "Ktor Server"
            validate { credentials ->
                if (credentials.name == credentials.password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    
        form(name = "myauth2") {
            userParamName = "user"
            passwordParamName = "password"
            challenge {
                /**/
            }
        }
    }
}
