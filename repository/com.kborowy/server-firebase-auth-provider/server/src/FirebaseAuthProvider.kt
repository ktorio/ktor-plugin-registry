/**
 * slot://io.ktor/server-core/security
 */
package kastle

import io.ktor.server.application.*
import io.ktor.server.auth.*
import java.io.File
import com.kborowy.authprovider.firebase.firebase

fun Application.configureFirebaseAuthProvider() {
    // export your firebase admin credentials to get started
    val myAdminFile = File("firebase-adminsdk.json")
    if (!myAdminFile.exists()) return

    install(Authentication) {
        firebase {
            setup {
                adminFile = myAdminFile
            }
            realm = "My Server"
            validate { token ->
                MyAuthenticatedUser(id = token.uid)
            }
        }
    }
}
