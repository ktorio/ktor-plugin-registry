/**
 * slot://io.ktor/server-core/security
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.ldap.*
import io.ktor.server.response.*

fun Application.configureAuthLdap() {
    val localhost = "http://0.0.0.0"
        val ldapServerPort = 6998 // TODO: change to real value!
        authentication {
        basic("authName") {
            realm = "realm"
            validate { credential ->
                ldapAuthenticate(credential, "ldap://$localhost:${ldapServerPort}", "uid=%s,ou=system")
            }
        }
    }
}
